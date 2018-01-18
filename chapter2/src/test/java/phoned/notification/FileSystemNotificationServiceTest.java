package phoned.notification;

import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemNotificationServiceTest {
    /**
     * Increase this value to make the tests more stable
     * Decrease this value to make the tests faster
     */
    private static final int TICK_INTERVAL = 50;
    private static final int HALF_A_TICK = TICK_INTERVAL / 2;

    private MockFileSystem fileSystem = new MockFileSystem();
    private FileSystemNotificationService notificationService =
            new FileSystemNotificationService(fileSystem, TICK_INTERVAL);

    private TestSubscriber<Notification> testSubscriber = TestSubscriber.create();

    @Before
    public void setUp() throws Exception {
        notificationService.init();
    }

    @Test
    public void theNotificationServiceReturnsTheCorrectNotifications() throws Exception {
        fileSystem.mockFiles(1, 2, 3);

        notificationService.getNotifications()
                .take(3)
                .subscribe(testSubscriber);

        Thread.sleep(TICK_INTERVAL);

        assertFile123();
    }

    @Test
    public void filesArePickedUpOnlyOnce() throws Exception {
        fileSystem.mockFiles(1);

        notificationService.getNotifications()
                .subscribe(testSubscriber);

        Thread.sleep(HALF_A_TICK);
        fileSystem.mockFiles(1, 2);
        Thread.sleep(TICK_INTERVAL);
        fileSystem.mockFiles(1, 2, 3);
        Thread.sleep(TICK_INTERVAL);

        testSubscriber.unsubscribe();

        assertFile123();
    }

    @Test
    public void whenAFileHasBeenPickedUp_andThenDisappears_andThenReappears_thenItIsPickedUpAgain() throws Exception {
        fileSystem.mockFiles(1);

        notificationService.getNotifications()
                .subscribe(testSubscriber);

        Thread.sleep(HALF_A_TICK);
        fileSystem.mockFiles();
        Thread.sleep(TICK_INTERVAL);
        fileSystem.mockFiles(1);
        Thread.sleep(TICK_INTERVAL);

        testSubscriber.unsubscribe();

        assertFile1Twice();
    }

    @Test
    public void theClockServiceHandlesExceptionsGracefully() throws Exception {
        fileSystem.doThrow(new IOException("Stuff went wrong, I guess"));

        notificationService.getNotifications()
                .subscribe(testSubscriber);

        Thread.sleep(HALF_A_TICK);
        Thread.sleep(TICK_INTERVAL);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(IOException.class);
    }

    @Test
    public void theObservableStopsRunningWhenTheSubscriberUnsubscribes() throws Exception {
        fileSystem.mockFiles(1);

        notificationService.getNotifications()
                .subscribe(testSubscriber);

        Thread.sleep(HALF_A_TICK);
        testSubscriber.assertValueCount(1);
        testSubscriber.unsubscribe();

        Thread.sleep(TICK_INTERVAL);

        testSubscriber.unsubscribe();

        assertThat(fileSystem.getNbOfWalkInvocations()).isEqualTo(1);
    }

    @Test
    public void theClockIsOnlyPolledFromOneSource() throws Exception {
        fileSystem.mockFiles(1);

        notificationService.getNotifications()
                .subscribe(testSubscriber);
        notificationService.getNotifications()
                .subscribe(testSubscriber);

        Thread.sleep(HALF_A_TICK);
        testSubscriber.assertValueCount(2);
        testSubscriber.unsubscribe();

        assertThat(fileSystem.getNbOfWalkInvocations()).isEqualTo(1);
    }

    private void assertFile123() {
        assertThat(testSubscriber.getOnNextEvents())
                .extracting(n -> n.title,n -> n.body)
                .containsExactly(
                        Tuple.tuple(MockFileSystem.title(1), MockFileSystem.body(1)),
                        Tuple.tuple(MockFileSystem.title(2), MockFileSystem.body(2)),
                        Tuple.tuple(MockFileSystem.title(3), MockFileSystem.body(3))
                );

        assertThat(testSubscriber.getOnNextEvents())
                .extracting(notification -> {
                    String[] pieces = notification.id.split("\\|/");
                    return pieces[pieces.length - 1];
                })
                .containsExactly(
                        MockFileSystem.filename(1),
                        MockFileSystem.filename(2),
                        MockFileSystem.filename(3)
                );
    }

    private void assertFile1Twice() {
        assertThat(testSubscriber.getOnNextEvents())
                .extracting(n -> n.title,n -> n.body)
                .contains(
                        Tuple.tuple(MockFileSystem.title(1), MockFileSystem.body(1)),
                        Tuple.tuple(MockFileSystem.title(1), MockFileSystem.body(1))
                );

        assertThat(testSubscriber.getOnNextEvents())
                .extracting(notification -> {
                    String[] pieces = notification.id.split("\\|/");
                    return pieces[pieces.length - 1];
                })
                .contains(
                        MockFileSystem.filename(1),
                        MockFileSystem.filename(1)
                );
    }
}