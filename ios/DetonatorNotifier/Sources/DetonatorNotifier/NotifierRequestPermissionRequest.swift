import UserNotifications

import Detonator

class NotifierRequestPermissionRequest: Request {
    public override func run() -> Void {
        DispatchQueue.global(qos: .userInitiated).async {
            let notificationCenter = UNUserNotificationCenter.current()
            
            notificationCenter.getNotificationSettings { settings in
                DispatchQueue.main.async {
                    switch settings.authorizationStatus {
                    case .authorized, .provisional, .ephemeral:
                        self.end(data: true)
                        
                    case .notDetermined:
                        notificationCenter.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                            DispatchQueue.main.async {
                                self.end(data: granted)
                            }
                        }
                        
                    case .denied:
                        self.end(data: false)
                        
                    @unknown default:
                        self.end(data: false)
                    }
                }
            }
        }
    }
}
