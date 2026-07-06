import UIKit
import WebKit

class GameViewController: UIViewController, WKScriptMessageHandler {

    private var webView: WKWebView!
    private var store: StoreManager!

    override var prefersStatusBarHidden: Bool { true }
    override var prefersHomeIndicatorAutoHidden: Bool { true }
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask { .landscape }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 5/255, green: 4/255, blue: 24/255, alpha: 1)
        UIApplication.shared.isIdleTimerDisabled = true

        // configure WKWebView with JS message handler
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        let controller = WKUserContentController()
        controller.add(self, name: "buy")
        config.userContentController = controller

        webView = WKWebView(frame: view.bounds, configuration: config)
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        webView.backgroundColor = .clear
        webView.isOpaque = false
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        if #available(iOS 16.4, *) { webView.isInspectable = true }
        view.addSubview(webView)

        // load game
        if let url = Bundle.main.url(forResource: "emberstorm", withExtension: "html") {
            webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }

        // set up StoreKit
        store = StoreManager { [weak self] event in
            self?.handleStoreEvent(event)
        }
        Task { await store.loadProducts() }
    }

    // JS → native bridge: game calls window.webkit.messageHandlers.buy.postMessage("sku")
    func userContentController(_ userContentController: WKUserContentController,
                               didReceive message: WKScriptMessage) {
        guard message.name == "buy", let sku = message.body as? String else { return }
        Task { await store.purchase(sku) }
    }

    private func handleStoreEvent(_ event: StoreManager.Event) {
        DispatchQueue.main.async { [weak self] in
            guard let self, let wv = self.webView else { return }
            switch event {
            case .granted(let sku):
                wv.evaluateJavaScript("window.grantEntitlement&&window.grantEntitlement('\(sku)')")
            case .coins(let n):
                wv.evaluateJavaScript("window.grantCoins&&window.grantCoins(\(n))")
            case .prices(let json):
                wv.evaluateJavaScript("window.setPrices&&window.setPrices(\(json))")
            case .failed:
                wv.evaluateJavaScript("window.purchaseFailed&&window.purchaseFailed()")
            }
        }
    }
}
