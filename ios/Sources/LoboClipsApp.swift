#if os(iOS)
import SwiftUI
import AVKit
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore

@main
struct TaviqoApp: App {
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            TaviqoRootView()
                .preferredColorScheme(.dark)
        }
    }
}

struct TaviqoClipItem: Identifiable, Codable {
    var id: String
    var authorUsername: String
    var caption: String
    var videoUrl: String
    var likesCount: Int
    var viewsCount: Int
}

final class TaviqoFeedViewModel: ObservableObject {
    @Published var clips: [TaviqoClipItem] = []
    private let db = Firestore.firestore(
        database: "ai-studio-android-loboclip-f031fd05-ba55-4b71-8396-2e4f1629a0fa"
    )
    private var listener: ListenerRegistration?

    func startListening() {
        guard Auth.auth().currentUser != nil else { return }
        listener = db.collection("clips")
            .whereField("status", isEqualTo: "active")
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                self?.clips = docs.map { doc in
                    let data = doc.data()
                    return TaviqoClipItem(
                        id: doc.documentID,
                        authorUsername: data["authorUsername"] as? String ?? "creador",
                        caption: data["caption"] as? String ?? "",
                        videoUrl: data["videoUrl"] as? String ?? "",
                        likesCount: data["likesCount"] as? Int ?? 0,
                        viewsCount: data["viewsCount"] as? Int ?? 0
                    )
                }
            }
    }
}

struct TaviqoRootView: View {
    @StateObject private var vm = TaviqoFeedViewModel()

    var body: some View {
        NavigationStack {
            ZStack {
                Color(red: 0.02, green: 0.03, blue: 0.04).ignoresSafeArea()
                if vm.clips.isEmpty {
                    VStack(spacing: 12) {
                        Text("TAVIQO")
                            .font(.title.bold())
                            .foregroundColor(Color(red: 0, green: 0.94, blue: 1))
                        Text("El mundo es tu escenario.")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.85))
                        Text("Inicia sesión con Google/Firebase iOS para explorar el escenario real.")
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                } else {
                    TabView {
                        ForEach(vm.clips) { clip in
                            VStack(alignment: .leading, spacing: 10) {
                                if let url = URL(string: clip.videoUrl), url.scheme == "https" {
                                    VideoPlayer(player: AVPlayer(url: url))
                                        .aspectRatio(9 / 16, contentMode: .fit)
                                        .clipShape(RoundedRectangle(cornerRadius: 18))
                                }
                                Text("@\(clip.authorUsername)")
                                    .foregroundColor(Color(red: 0, green: 0.94, blue: 1))
                                    .bold()
                                Text(clip.caption)
                                    .foregroundColor(.white)
                            }
                            .padding()
                        }
                    }
                    .tabViewStyle(.page)
                }
            }
            .onAppear { vm.startListening() }
        }
    }
}
#endif
