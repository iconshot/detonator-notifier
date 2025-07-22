import UIKit

import Detonator

class NotifierRegisterForRemoteMessagesRequest: Request {
    public override func run() -> Void {
        UIApplication.shared.registerForRemoteNotifications()
        
        end()
    }
}
