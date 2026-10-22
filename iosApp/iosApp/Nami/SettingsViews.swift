import SwiftUI

// Account e impostazioni, come il profilo di Apple Music: in cima l'account di YouTube Music
// (accesso con Google), poi riproduzione, contenuti, spazio, privacy e informazioni.

/// L'iniziale del tuo nome in un cerchio col colore dell'accento (o l'omino se non hai fatto l'accesso).
struct AccountAvatar: View {
    var size: CGFloat = 30
    @ObservedObject private var settings = SettingsStore.shared

    var body: some View {
        Group {
            if let account = settings.account {
                Text(String(account.name.prefix(1)).uppercased())
                    .font(.system(size: size * 0.48, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)
                    .frame(width: size, height: size)
                    .background(Circle().fill(.tint))
            } else {
                Image(systemName: "person.crop.circle")
                    .font(.system(size: size * 0.8, weight: .regular))
                    .frame(width: size, height: size)
            }
        }
        .contentTransition(.opacity)
    }
}

struct SettingsView: View {
    @ObservedObject private var app = NamiApp.shared
    @ObservedObject private var settings = SettingsStore.shared
    @ObservedObject private var library = LibraryStore.shared
    @ObservedObject private var downloads = DownloadsStore.shared
    @Environment(\.dismiss) private var dismiss

    @State private var loggingIn = false
    @State private var confirmLogout = false
    @State private var confirmDeleteDownloads = false
    @State private var confirmClearHistory = false

    var body: some View {
        NavigationStack {
            Form {
                accountSection

                Section {
                    HStack(spacing: 0) {
                        Stat(value: library.liked.count, label: "Preferiti", systemImage: "heart.fill")
                        Divider().frame(height: 36)
                        Stat(value: library.playlists.count, label: "Playlist", systemImage: "music.note.list")
                        Divider().frame(height: 36)
                        Stat(value: downloads.songs.count, label: "Scaricati", systemImage: "arrow.down.circle.fill")
                    }
                    .padding(.vertical, 4)
                }

                Section(L("Riproduzione")) {
                    Picker(selection: $settings.audioQuality) {
                        ForEach(Array(SettingsStore.qualities.enumerated()), id: \.offset) { index, label in
                            Text(label).tag(index)
                        }
                    } label: {
                        SettingsLabel(L("Qualità audio"), systemImage: "waveform", color: .orange)
                    }
                }

                Section {
                    NavigationLink {
                        OptionPicker(title: "Lingua dei contenuti", options: settings.languages, selection: $settings.language)
                    } label: {
                        LabeledContent {
                            Text(settings.languageName(settings.language))
                        } label: {
                            SettingsLabel(L("Lingua dei contenuti"), systemImage: "character.bubble", color: .blue)
                        }
                    }
                    NavigationLink {
                        OptionPicker(title: "Paese dei contenuti", options: settings.countries, selection: $settings.country)
                    } label: {
                        LabeledContent {
                            Text(settings.countryName(settings.country))
                        } label: {
                            SettingsLabel(L("Paese dei contenuti"), systemImage: "globe.europe.africa.fill", color: .green)
                        }
                    }
                } header: {
                    Text(L("Contenuti"))
                } footer: {
                    Text(L("Decidono la Home, le classifiche e i consigli di YouTube Music."))
                }

                Section {
                    LabeledContent {
                        Text(ByteCountFormatter.string(fromByteCount: settings.downloadsBytes, countStyle: .file))
                            .contentTransition(.numericText())
                    } label: {
                        SettingsLabel(L("Spazio dei download"), systemImage: "internaldrive.fill", color: .gray)
                    }
                    Button(role: .destructive) {
                        confirmDeleteDownloads = true
                    } label: {
                        SettingsLabel(L("Elimina tutti i download"), systemImage: "trash.fill", color: .red)
                    }
                    .disabled(downloads.songs.isEmpty)
                } header: {
                    Text(L("Spazio"))
                }

                Section(L("Privacy")) {
                    Button(role: .destructive) {
                        confirmClearHistory = true
                    } label: {
                        SettingsLabel(L("Cancella cronologia ricerche"), systemImage: "clock.arrow.circlepath", color: .purple)
                    }
                }

                Section {
                    Picker(L("Lingua dell'app"), selection: Binding(get: { app.language }, set: { app.setLanguage($0) })) {
                        Text(L("Sistema")).tag(0)
                        Text("Italiano").tag(1)
                        Text("English").tag(2)
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                } header: {
                    Text(L("Lingua dell'app"))
                }

                Section {
                    Toggle(isOn: Binding(get: { app.iosUI }, set: { app.setIosUI($0) })) {
                        SettingsLabel(L("Interfaccia iOS (Liquid Glass)"), systemImage: "sparkles", color: .indigo)
                    }
                } header: {
                    Text(L("Aspetto"))
                } footer: {
                    Text(L("Spenta, Nami torna all'interfaccia di sempre (con tutte le personalizzazioni di colori e player)."))
                }

                Section {
                    LabeledContent(L("Versione"), value: LF("Nami · motore {0}", settings.version))
                }
            }
            .navigationTitle(L("Account"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("Fine")) { dismiss() }
                }
            }
            .sheet(isPresented: $loggingIn) {
                NavigationStack {
                    GoogleLoginView(onDone: { loggingIn = false })
                        .ignoresSafeArea(edges: .bottom)
                        .navigationTitle(L("Accedi con Google"))
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button(L("Annulla")) { loggingIn = false }
                            }
                        }
                }
                .namiTint()
            }
            .confirmationDialog(L("Uscire dall'account?"), isPresented: $confirmLogout, titleVisibility: .visible) {
                Button(L("Esci"), role: .destructive) { settings.logout() }
            } message: {
                Text(L("La Home e le playlist torneranno quelle senza account. I brani scaricati restano."))
            }
            .confirmationDialog(L("Eliminare tutti i download?"), isPresented: $confirmDeleteDownloads, titleVisibility: .visible) {
                Button(L("Elimina"), role: .destructive) { settings.removeAllDownloads() }
            } message: {
                Text(L("I brani scaricati vengono tolti dal telefono. Potrai riscaricarli quando vuoi."))
            }
            .confirmationDialog(L("Cancellare la cronologia delle ricerche?"), isPresented: $confirmClearHistory, titleVisibility: .visible) {
                Button(L("Cancella"), role: .destructive) { settings.clearSearchHistory() }
            }
            .onAppear { settings.refreshDownloadsSize() }
        }
    }

    @ViewBuilder
    private var accountSection: some View {
        Section {
            if let account = settings.account {
                HStack(spacing: 14) {
                    AccountAvatar(size: 58)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(account.name)
                            .font(.title3.weight(.semibold))
                        if let email = account.email {
                            Text(email)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        Text(L("Collegato a YouTube Music"))
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.tint)
                    }
                }
                .padding(.vertical, 4)
                Button(role: .destructive) {
                    confirmLogout = true
                } label: {
                    Text(L("Esci dall'account"))
                }
            } else {
                HStack(spacing: 14) {
                    AccountAvatar(size: 58)
                        .foregroundStyle(.secondary)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("Ospite"))
                            .font(.title3.weight(.semibold))
                        Text(L("Accedi per avere la tua Home, le tue playlist e i tuoi Mi piace di YouTube Music."))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 4)
                Button {
                    loggingIn = true
                } label: {
                    Label(L("Accedi con Google"), systemImage: "person.badge.key.fill")
                        .font(.body.weight(.semibold))
                }
            }
        }
    }
}

/// Icona colorata nel quadratino arrotondato, come nelle Impostazioni di iOS.
private struct SettingsLabel: View {
    let title: String
    let systemImage: String
    let color: Color

    init(_ title: String, systemImage: String, color: Color) {
        self.title = title
        self.systemImage = systemImage
        self.color = color
    }

    var body: some View {
        Label {
            Text(L(title))
                .foregroundStyle(.primary)
        } icon: {
            Image(systemName: systemImage)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 29, height: 29)
                .background(color.gradient, in: RoundedRectangle(cornerRadius: 7, style: .continuous))
        }
    }
}

private struct Stat: View {
    let value: Int
    let label: String
    let systemImage: String

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: systemImage)
                .font(.headline)
                .foregroundStyle(.tint)
            Text("\(value)")
                .font(.title3.weight(.bold).monospacedDigit())
                .contentTransition(.numericText())
            Text(L(label))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

/// Elenco lungo con ricerca (lingue, paesi).
private struct OptionPicker: View {
    let title: String
    let options: [OptionItem]
    @Binding var selection: String
    @State private var query = ""
    @Environment(\.dismiss) private var dismiss

    private var filtered: [OptionItem] {
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return options }
        return options.filter { $0.name.localizedCaseInsensitiveContains(q) }
    }

    var body: some View {
        List(filtered) { option in
            Button {
                selection = option.id
                dismiss()
            } label: {
                HStack {
                    Text(option.name)
                        .foregroundStyle(.primary)
                    Spacer()
                    if option.id == selection {
                        Image(systemName: "checkmark")
                            .font(.body.weight(.semibold))
                            .foregroundStyle(.tint)
                    }
                }
            }
        }
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always))
        .navigationTitle(L(title))
        .navigationBarTitleDisplayMode(.inline)
    }
}
