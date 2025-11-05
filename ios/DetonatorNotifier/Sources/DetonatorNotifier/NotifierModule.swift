import UIKit
import UserNotifications

import Firebase
import FirebaseMessaging

import Detonator

public class NotifierModule: Module, UNUserNotificationCenterDelegate, MessagingDelegate {
    private var token: String = ""
    
    public override func setUp() -> Void {
        FirebaseApp.configure()
        
        DispatchQueue.global(qos: .userInitiated).async {
            let notificationCenter = UNUserNotificationCenter.current()
            
            notificationCenter.delegate = self
        }
        
        Messaging.messaging().delegate = self
        
        UIApplication.shared.setMinimumBackgroundFetchInterval(UIApplication.backgroundFetchIntervalMinimum)
        
        NotificationCenter.default.addObserver(forName: .apnsTokenReceived, object: nil, queue: .main) { notification in
            guard let apnsToken = notification.object as? Data else {
                return
            }
            
            Messaging.messaging().apnsToken = apnsToken
        }
        
        NotificationCenter.default.addObserver(forName: .remoteMessageReceived, object: nil, queue: .main) { notification in
            guard let dictionary = notification.object as? [AnyHashable: Any] else {
                return
            }
            
            var data = dictionary.reduce(into: [String: String]()) { result, item in
                guard let key = item.key as? String else {
                    return
                }
                
                guard let value = item.value as? String else {
                    return
                }
                
                switch key {
                case "google.c.fid", "gcm.message_id", "google.c.sender.id":
                    break
                    
                default:
                    result[key] = value
                    break
                }
            }
            
            guard let messageId = dictionary["gcm.message_id"] as? CustomStringConvertible else {
                return
            }
            
            guard let senderId = dictionary["google.c.sender.id"] as? CustomStringConvertible else {
                return
            }
            
            let message = Message(messageId: messageId.description, senderId: senderId.description, data: data)
            
            self.detonator.send("com.iconshot.detonator.notifier.message", message)
        }
        
        detonator.setRequestListener("com.iconshot.detonator.notifier::requestPermission") { promise, value, edge in
            DispatchQueue.global(qos: .userInitiated).async {
                let notificationCenter = UNUserNotificationCenter.current()
                
                notificationCenter.getNotificationSettings { settings in
                    DispatchQueue.main.async {
                        switch settings.authorizationStatus {
                        case .authorized, .provisional, .ephemeral:
                            promise.resolve(true)
                            
                        case .notDetermined:
                            notificationCenter.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                                promise.resolve(granted)
                            }
                            
                        case .denied:
                            promise.resolve(false)
                            
                        @unknown default:
                            promise.resolve(false)
                        }
                    }
                }
            }
        }
        
        detonator.setRequestListener("com.iconshot.detonator.notifier::checkPermission") { promise, value, edge in
            DispatchQueue.global(qos: .userInitiated).async {
                let notificationCenter = UNUserNotificationCenter.current()
                
                notificationCenter.getNotificationSettings { settings in
                    DispatchQueue.main.async {
                        switch settings.authorizationStatus {
                        case .authorized, .provisional, .ephemeral:
                            promise.resolve(true)
                            
                        @unknown default:
                            promise.resolve(false)
                        }
                    }
                }
            }
        }
        
        detonator.setRequestListener("com.iconshot.detonator.notifier::registerForRemoteMessages") { promise, value, edge in
            UIApplication.shared.registerForRemoteNotifications()
            
            promise.resolve()
        }
        
        detonator.setRequestListener("com.iconshot.detonator.notifier::showNotification") { promise, value, edge in
            let data: ShowNotificationData = self.detonator.decode(value)!
            
            DispatchQueue.global(qos: .userInitiated).async {
                let notificationCenter = UNUserNotificationCenter.current()
                
                let content = UNMutableNotificationContent()
                
                content.title = data.title
                
                if let body = data.body {
                    content.body = body
                }
                
                content.sound = .default
                
                if let pictureUrl = data.pictureUrl {
                    if let url = URL(string: pictureUrl) {
                        var errorMessage: String?
                        
                        let semaphore = DispatchSemaphore(value: 0)
                        
                        var attachments: [UNNotificationAttachment] = []
                        
                        let task = URLSession.shared.downloadTask(with: url) { tempURL, _, _ in
                            defer {
                                semaphore.signal()
                            }

                            guard let tempURL = tempURL else {
                                errorMessage = "Failed to download image."
                                
                                return
                            }
                            
                            let targetFileName = UUID().uuidString + ".jpg"
                            
                            let targetURL = FileManager.default.temporaryDirectory.appendingPathComponent(targetFileName)
                            
                            do {
                                try FileManager.default.moveItem(at: tempURL, to: targetURL)
                            } catch {
                                errorMessage = "Failed to move downloaded image."
                                
                                return
                            }
                            
                            do {
                                let attachment = try UNNotificationAttachment(identifier: "image", url: targetURL, options: nil)
                                
                                attachments.append(attachment)
                            } catch {
                                errorMessage = "Failed to create notification attachment from image."
                                
                                return
                            }
                        }
                        
                        task.resume()
                        
                        semaphore.wait()
                        
                        if let errorMessage = errorMessage {
                            promise.reject(errorMessage)
                            
                            return
                        }
                        
                        content.attachments = attachments
                    }
                }

                let identifier = String(data.id)

                let request = UNNotificationRequest(identifier: identifier, content: content, trigger: nil)

                notificationCenter.add(request) { error in
                    if let error = error {
                        promise.reject(error)
                        
                        return
                    }
                    
                    promise.resolve()
                }
            }
        }
    }
    
    private func setToken(_ token: String) -> Void {
        if self.token == token {
            return
        }
        
        self.token = token
        
        detonator.send("com.iconshot.detonator.notifier.token", token)
    }
    
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.alert, .sound, .badge])
    }
    
    @objc public func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        setToken(fcmToken ?? "")
    }
    
    public struct Message: Encodable {
        let messageId: String
        let senderId: String
        let data: [String: String]
    }
    
    public struct ShowNotificationData: Decodable {
        let id: Int
        let title: String
        let body: String?
        let pictureUrl: String?
    }
}

extension Notification.Name {
    public static let apnsTokenReceived = Notification.Name("apnsTokenReceived")
    public static let remoteMessageReceived = Notification.Name("remoteMessageReceived")
}
