import UserNotifications

import Detonator

class NotifierShowNotificationRequest: Request {
    public override func run() -> Void {
        let data: ShowNotificationData = decodeData()!
        
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
                        DispatchQueue.main.async {
                            self.error(message: errorMessage)
                        }
                        
                        return
                    }
                    
                    content.attachments = attachments
                }
            }

            let identifier = String(data.id)

            let request = UNNotificationRequest(identifier: identifier, content: content, trigger: nil)

            notificationCenter.add(request) { error in
                DispatchQueue.main.async {
                    if let error = error {
                        self.error(error: error)
                        
                        return
                    }
                    
                    self.end()
                }
            }
        }
    }
    
    struct ShowNotificationData: Decodable {
        let id: Int
        let title: String
        let body: String?
        let pictureUrl: String?
    }
}
