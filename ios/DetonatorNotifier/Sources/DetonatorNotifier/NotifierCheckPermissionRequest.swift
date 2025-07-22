import UserNotifications

import Detonator

class NotifierCheckPermissionRequest: Request {
    public override func run() -> Void {
        DispatchQueue.global(qos: .userInitiated).async {
            let notificationCenter = UNUserNotificationCenter.current()
            
            notificationCenter.getNotificationSettings { settings in
                DispatchQueue.main.async {
                    switch settings.authorizationStatus {
                    case .authorized, .provisional, .ephemeral:
                        self.end(data: true)
                        
                    @unknown default:
                        self.end(data: false)
                    }
                }
            }
        }
    }
}
