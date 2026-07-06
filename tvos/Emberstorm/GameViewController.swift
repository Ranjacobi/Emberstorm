import UIKit
import WebKit
import GameController

/// tvOS game controller: bridges Siri Remote input → JavaScript keyboard events.
/// Select/Click → Space (flip in game, activate buttons in menus)
/// Play/Pause → Space
/// Menu → Escape (back)
/// D-pad → Arrow keys (menu navigation)
/// Swipe up/down on trackpad → Space (flip)
class GameViewController: UIViewController, WKNavigationDelegate {

    private var webView: WKWebView!

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 5/255, green: 4/255, blue: 24/255, alpha: 1)

        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true

        webView = WKWebView(frame: view.bounds, configuration: config)
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        webView.backgroundColor = .clear
        webView.isOpaque = false
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.navigationDelegate = self
        view.addSubview(webView)

        if let url = Bundle.main.url(forResource: "emberstorm", withExtension: "html") {
            webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }

        // set up game controller (Siri Remote)
        setupControllers()
        NotificationCenter.default.addObserver(self, selector: #selector(controllerConnected), name: .GCControllerDidConnect, object: nil)

        // tap gesture on the remote trackpad as backup
        let tap = UITapGestureRecognizer(target: self, action: #selector(remoteTapped))
        tap.allowedPressTypes = [NSNumber(value: UIPress.PressType.select.rawValue)]
        view.addGestureRecognizer(tap)

        // swipe up/down = flip
        for dir: UISwipeGestureRecognizer.Direction in [.up, .down] {
            let swipe = UISwipeGestureRecognizer(target: self, action: #selector(remoteSwiped))
            swipe.direction = dir
            view.addGestureRecognizer(swipe)
        }
    }

    // MARK: — Siri Remote via GCController

    private func setupControllers() {
        for controller in GCController.controllers() { configure(controller) }
    }

    @objc private func controllerConnected(_ note: Notification) {
        if let c = note.object as? GCController { configure(c) }
    }

    private func configure(_ controller: GCController) {
        controller.microGamepad?.buttonA.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.injectKey(" ") }
        }
        controller.microGamepad?.buttonMenu.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.injectKey("Escape") }
        }
        // D-pad for menu navigation
        controller.microGamepad?.dpad.up.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.injectKey("ArrowUp") }
        }
        controller.microGamepad?.dpad.down.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.injectKey("ArrowDown") }
        }
        controller.microGamepad?.dpad.left.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.injectKey("ArrowLeft") }
        }
        controller.microGamepad?.dpad.right.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { self?.injectKey("ArrowRight") }
        }
    }

    // MARK: — Gesture fallbacks

    @objc private func remoteTapped() { injectKey(" ") }
    @objc private func remoteSwiped() { injectKey(" ") }

    // MARK: — Press events (backup for controllers)

    override func pressesBegan(_ presses: Set<UIPress>, with event: UIPressesEvent?) {
        for press in presses {
            switch press.type {
            case .select, .playPause: injectKey(" ")
            case .menu: injectKey("Escape")
            case .upArrow: injectKey("ArrowUp")
            case .downArrow: injectKey("ArrowDown")
            case .leftArrow: injectKey("ArrowLeft")
            case .rightArrow: injectKey("ArrowRight")
            default: super.pressesBegan(presses, with: event)
            }
        }
    }

    // MARK: — JS bridge

    private func injectKey(_ key: String) {
        let js = "window.dispatchEvent(new KeyboardEvent('keydown',{key:'\(key)',bubbles:true}));"
        webView.evaluateJavaScript(js)
    }
}
