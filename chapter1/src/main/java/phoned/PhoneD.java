package phoned;

import phoned.clock.ClockController;
import phoned.clock.ClockWidget;
import phoned.clock.ClockService;
import phoned.notification.FileSystem;
import phoned.notification.FileSystemNotificationService;
import phoned.notification.NotificationController;
import phoned.notification.NotificationWidget;

import java.time.Clock;

public class PhoneD {
    public static void main(String[] args) throws Exception {
        ClockWidget clockWidget = new ClockWidget();
        ClockService clockService = new ClockService(Clock.systemDefaultZone(), 1000);
        ClockController clockController = new ClockController(clockWidget, clockService);

        NotificationWidget notificationWidget = new NotificationWidget();
        FileSystemNotificationService notificationService = new FileSystemNotificationService(new FileSystem());
        NotificationController notificationController = new NotificationController(notificationWidget, notificationService);

        Window window = new Window(clockWidget);
        window.init();

        clockService.init();
        notificationService.init();

        clockController.init();
        notificationController.init();

        window.show();

        clockService.printCurrentTime();
        notificationService.printDirectoryAndContent();
    }
}
