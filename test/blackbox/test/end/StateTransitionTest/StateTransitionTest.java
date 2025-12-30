package test.end.StateTransitionTest;

import org.junit.After;
import org.junit.Test;

// 关键：必须引入原来那个包里的工具类，否则会报红
import test.endtoend.auctionsniper.ApplicationRunner;
import test.endtoend.auctionsniper.FakeAuctionServer;

/**
 * 【黑盒测试】状态转换测试
 * 对应禅道需求：验证拍卖狙击手在不同竞价场景下的状态机逻辑
 *
 * 包含两个核心用例：
 * 1. 完整竞价流程（反超获胜）
 * 2. 止损流程（放弃竞拍）
 */
public class StateTransitionTest {

    // 准备测试环境：模拟商品 item-54321
    private final FakeAuctionServer auction = new FakeAuctionServer("item-54321");
    // 准备测试环境：启动被测程序
    private final ApplicationRunner application = new ApplicationRunner();

    /**
     * 用例 1：反败为胜路径验证
     * 路径：JOINING -> BIDDING -> WINNING -> (被反超) -> BIDDING -> WINNING -> WON
     */
    @Test
    public void testStory_BidWarAndWin() throws Exception {
        // [步骤 1] 拍卖开始
        auction.startSellingItem();

        // [步骤 2] 狙击手加入
        // 预期状态：JOINING
        application.startBiddingIn(auction);
        auction.hasReceivedJoinRequestFrom(ApplicationRunner.SNIPER_XMPP_ID);

        // [步骤 3] 首次出价
        // 场景：有人出价 1000 -> 我方应出价 1098
        // 预期状态：BIDDING
        auction.reportPrice(1000, 98, "other bidder");
        application.hasShownSniperIsBidding(auction, 1000, 1098);
        auction.hasReceivedBid(1098, ApplicationRunner.SNIPER_XMPP_ID);

        // [步骤 4] 取得领先
        // 场景：确认我方出价有效
        // 预期状态：WINNING
        auction.reportPrice(1098, 97, ApplicationRunner.SNIPER_XMPP_ID);
        application.hasShownSniperIsWinning(auction, 1098);

        // [步骤 5] 被反超 (测试关键点)
        // 场景：对手出价 1200，但我方还能承受
        // 预期状态：从 WINNING 变回 BIDDING
        auction.reportPrice(1200, 100, "bad guy");
        application.hasShownSniperIsBidding(auction, 1200, 1300);
        auction.hasReceivedBid(1300, ApplicationRunner.SNIPER_XMPP_ID);

        // [步骤 6] 再次领先
        // 预期状态：WINNING
        auction.reportPrice(1300, 99, ApplicationRunner.SNIPER_XMPP_ID);
        application.hasShownSniperIsWinning(auction, 1300);

        // [步骤 7] 拍卖结束
        // 预期状态：WON
        auction.announceClosed();
        application.hasShownSniperHasWonAuction(auction, 1300);
    }

    /**
     * 用例 2：高价止损路径验证
     * 路径：WINNING -> LOSING -> LOST
     */
    @Test
    public void testStory_StopPriceExceeded() throws Exception {
        auction.startSellingItem();

        // [步骤 1] 设置止损价为 1500
        application.startBiddingWithStopPrice(auction, 1500);
        auction.hasReceivedJoinRequestFrom(ApplicationRunner.SNIPER_XMPP_ID);

        // [步骤 2] 正常竞价
        auction.reportPrice(1000, 98, "other bidder");
        application.hasShownSniperIsBidding(auction, 1000, 1098);
        auction.hasReceivedBid(1098, ApplicationRunner.SNIPER_XMPP_ID);

        // [步骤 3] 取得领先
        auction.reportPrice(1098, 97, ApplicationRunner.SNIPER_XMPP_ID);
        application.hasShownSniperIsWinning(auction, 1098);

        // [步骤 4] 价格飙升，超过止损价 (测试关键点)
        // 场景：有人出价 2000 ( > 1500)
        // 预期状态：从 WINNING 变为 LOSING (且不再出价)
        auction.reportPrice(2000, 100, "rich guy");
        application.hasShownSniperIsLosing(auction, 2000, 1098);

        // [步骤 5] 拍卖结束
        // 预期状态：LOST
        auction.announceClosed();
        application.hasShownSniperHasLostAuction(auction, 2000, 1098);
    }

    // 清理工作：关闭窗口和连接
    @After
    public void tearDown() {
        auction.stop();
        application.stop();
    }
}