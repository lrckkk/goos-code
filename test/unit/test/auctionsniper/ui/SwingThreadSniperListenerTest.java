package test.auctionsniper.ui;

import javax.swing.SwingUtilities;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.concurrent.Synchroniser; // 1. 必须导入这个包！
import org.junit.Test;
import org.junit.runner.RunWith;

import auctionsniper.SniperListener;
import auctionsniper.SniperSnapshot;
import auctionsniper.SniperState;
import auctionsniper.ui.SwingThreadSniperListener;

@RunWith(JMock.class)
public class SwingThreadSniperListenerTest {

    // 2. 这里改了！给 Mockery 加了 Synchroniser，允许跨线程调用
    private final Mockery context = new Mockery() {{
        setThreadingPolicy(new Synchroniser());
    }};

    private final SniperListener delegate = context.mock(SniperListener.class);
    private final SwingThreadSniperListener sniper = new SwingThreadSniperListener(delegate);

    @Test
    public void delegatesUpdateToSwingEventDispatchThread() throws Exception {
        final SniperSnapshot snapshot = new SniperSnapshot("item-1", 123, 123, SniperState.WINNING);

        context.checking(new Expectations() {{
            oneOf(delegate).sniperStateChanged(snapshot);
        }});

        // 在主线程调用
        sniper.sniperStateChanged(snapshot);

        // 等待 Swing 线程执行完毕
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                // 这里的空代码是为了确保前面的任务执行完了
            }
        });
    }
}