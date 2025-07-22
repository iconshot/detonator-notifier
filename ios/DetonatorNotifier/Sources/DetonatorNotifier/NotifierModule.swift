import UIKit
import UserNotifications

import Firebase
import FirebaseMessaging

import Detonator

public class NotifierModule: Module, UNUserNotificationCenterDelegate, MessagingDelegate {
    private var token: String = ""
    
    public override func setUp() -> Void {
        detonator.setRequestClass("com.iconshot.detonator.notifier::showNotification", NotifierShowNotificationRequest.self)
        
        detonator.setRequestClass("com.iconshot.detonator.notifier::requestPermission", NotifierRequestPermissionRequest.self)
        detonator.setRequestClass("com.iconshot.detonator.notifier::checkPermission", NotifierCheckPermissionRequest.self)
        detonator.setRequestClass("com.iconshot.detonator.notifier::registerForRemoteMessages", NotifierRegisterForRemoteMessagesRequest.self)
        
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
            
            self.detonator.emit("com.iconshot.detonator.notifier.message", message)
        }
    }
    
    private func setToken(_ token: String) -> Void {
        if self.token == token {
            return
        }
        
        self.token = token
        
        detonator.emit("com.iconshot.detonator.notifier.token", token)
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
    
    struct Message: Encodable {
        let messageId: String
        let senderId: String
        let data: [String: String]
    }
}

extension Notification.Name {
    public static let apnsTokenReceived = Notification.Name("apnsTokenReceived")
    public static let remoteMessageReceived = Notification.Name("remoteMessageReceived")
}
