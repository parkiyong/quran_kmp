import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinModuleKt.initKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}