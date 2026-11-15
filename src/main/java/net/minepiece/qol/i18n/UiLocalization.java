package net.minepiece.qol.i18n;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class UiLocalization {
    public static final String DEFAULT_LANGUAGE = "en";

    private static final List<LanguageOption> SUPPORTED_LANGUAGES = List.of(
        new LanguageOption("en", "English"),
        new LanguageOption("fr", "Français"),
        new LanguageOption("es", "Español"),
        new LanguageOption("de", "Deutsch"),
        new LanguageOption("it", "Italiano"),
        new LanguageOption("pt", "Português"),
        new LanguageOption("pl", "Polski"),
        new LanguageOption("id", "Indonesia"),
        new LanguageOption("tr", "Türkçe")
    );

    private static final Map<String, String[]> STRINGS = new HashMap<>();

    static {
        add("bosses.unknown_island", "Unknown island", "Île inconnue", "Isla desconocida", "Unbekannte Insel", "Isola sconosciuta", "Ilha desconhecida", "Nieznana wyspa", "Pulau tidak diketahui", "Bilinmeyen ada");
        add("bosses.collapse_hint", "Chat: click island headings • Editor: right-click", "Chat : cliquez sur les îles • Éditeur : clic droit", "Chat: clic en islas • Editor: clic derecho", "Chat: Insel anklicken • Editor: Rechtsklick", "Chat: clic sui titoli delle isole • Editor: clic destro", "Chat: clique nas ilhas • Editor: botão direito", "Czat: kliknij wyspę • Edytor: prawy przycisk", "Chat: klik judul pulau • Editor: klik kanan", "Sohbet: ada başlığına tıkla • Düzenleyici: sağ tık");
        add("jobs.estimated_hint", "~LVL is estimated from the XP requirement", "~LVL est estimé selon l’XP requise", "~LVL se estima según la XP necesaria", "~LVL wird aus benötigter XP geschätzt", "~LVL è stimato dall’XP richiesta", "~LVL é estimado pelo XP necessário", "~LVL to szacunek na podstawie wymaganego XP", "~LVL diperkirakan dari kebutuhan XP", "~LVL gereken XP ile tahmin edilir");
        add("extras.title", "Extras", "Extras", "Extras", "Extras", "Extra", "Extras", "Dodatki", "Tambahan", "Ekstralar");
        add("hud.panel.daily_quests", "Daily quests", "Quêtes quotidiennes", "Misiones diarias", "Tägliche Quests", "Missioni giornaliere", "Missões diárias", "Codzienne zadania", "Misi harian", "Günlük görevler");
        add("hud.panel.active_pets", "Active pets", "Familiers actifs", "Mascotas activas", "Aktive Begleiter", "Pet attivi", "Mascotes ativos", "Aktywne zwierzaki", "Pet aktif", "Aktif evcil hayvanlar");
        add("extras.daily_quests", "Daily quests HUD", "HUD quêtes quotidiennes", "HUD misiones diarias", "Tägliche Quests HUD", "HUD missioni giornaliere", "HUD missões diárias", "HUD codziennych zadań", "HUD misi harian", "Günlük görev HUD");
        add("extras.active_pets", "Active pets HUD", "HUD familiers actifs", "HUD mascotas activas", "Aktive Begleiter HUD", "HUD pet attivi", "HUD mascotes ativos", "HUD aktywnych zwierzaków", "HUD pet aktif", "Aktif evcil hayvan HUD");
        add("extras.minion", "Minion resource calculator", "Calcul des ressources du minion", "Calculadora de recursos del minion", "Minion-Ressourcenrechner", "Calcolo risorse minion", "Calculadora de recursos do minion", "Kalkulator zasobów miniona", "Kalkulator sumber daya minion", "Minyon kaynak hesaplayıcı");
        add("extras.pet_rolls", "Pet roll percentages", "Pourcentages des familiers", "Porcentajes de mascotas", "Begleiter-Wertprozente", "Percentuali statistiche pet", "Porcentagens dos mascotes", "Procenty statystyk zwierzaków", "Persentase statistik pet", "Evcil hayvan istatistik yüzdeleri");
        add("extras.background", "Automatic menu refreshes", "Actualisation automatique", "Actualización automática", "Automatische Aktualisierung", "Aggiornamenti automatici", "Atualização automática", "Automatyczne odświeżanie", "Pembaruan otomatis", "Otomatik yenileme");
        add("extras.boss_alerts", "Boss notifications (30s)", "Notifications boss (30s)", "Avisos de jefes (30s)", "Boss-Hinweise (30s)", "Avvisi boss (30s)", "Avisos de chefes (30s)", "Powiadomienia bossów (30s)", "Notifikasi boss (30s)", "Boss bildirimleri (30s)");
        add("extras.rarity_filter", "Rarity filters", "Filtres de rareté", "Filtros de rareza", "Seltenheitsfilter", "Filtri rarità", "Filtros de raridade", "Filtry rzadkości", "Filter kelangkaan", "Nadirlik filtreleri");
        add("extras.sorting", "Storage sorting buttons", "Boutons de tri des coffres", "Botones para ordenar cofres", "Kisten-Sortierknöpfe", "Pulsanti ordine bauli", "Botões para ordenar baús", "Przyciski sortowania skrzyń", "Tombol urut peti", "Sandık sıralama düğmeleri");
        add("extras.hotbar", "Hotbar rarity badges", "Rareté dans la barre rapide", "Rareza en la barra rápida", "Seltenheit in der Schnellleiste", "Rarità nella barra rapida", "Raridade na barra rápida", "Rzadkość na pasku", "Kelangkaan di hotbar", "Hızlı çubuk nadirliği");
        add("extras.refresh", "Refresh data", "Actualiser", "Actualizar datos", "Daten aktualisieren", "Aggiorna dati", "Atualizar dados", "Odśwież dane", "Perbarui data", "Verileri yenile");
        add("extras.help", "Quick help", "Aide rapide", "Ayuda rápida", "Kurzhilfe", "Guida rapida", "Ajuda rápida", "Szybka pomoc", "Bantuan singkat", "Hızlı yardım");
        add("extras.no_active_pets", "No active pets on this page", "Aucun familier actif sur cette page", "Sin mascotas activas en esta página", "Keine aktiven Begleiter auf dieser Seite", "Nessun pet attivo in questa pagina", "Sem mascotes ativos nesta página", "Brak aktywnych zwierzaków na tej stronie", "Tidak ada pet aktif di halaman ini", "Bu sayfada aktif pet yok");
        add("extras.minion_maxed", "Minion: max prestige ✓", "Minion : prestige max ✓", "Minion: prestigio máximo ✓", "Minion: maximales Prestige ✓", "Minion: prestigio massimo ✓", "Minion: prestígio máximo ✓", "Minion: maksymalny prestiż ✓", "Minion: prestige maksimum ✓", "Minyon: en yüksek prestij ✓");
        add("extras.stacks", "stacks", "stacks", "stacks", "Stacks", "stack", "stacks", "stacków", "stack", "yığın");
        add("extras.next_prestige", "Next prestige", "Prestige suivant", "Siguiente prestigio", "Nächstes Prestige", "Prossimo prestigio", "Próximo prestígio", "Następny prestiż", "Prestige berikutnya", "Sonraki prestij");
        add("extras.max_prestige", "Max prestige (P10)", "Prestige max (P10)", "Prestigio máximo (P10)", "Maximales Prestige (P10)", "Prestigio massimo (P10)", "Prestígio máximo (P10)", "Maksymalny prestiż (P10)", "Prestige maksimum (P10)", "En yüksek prestij (P10)");
        add("extras.feed_hint", "Open the feeding menu to learn resource XP", "Ouvrez le menu de nourrissage pour calculer les stacks", "Abre el menú de alimentación para calcular stacks", "Fütterungsmenü öffnen, um Ressourcen-XP zu lernen", "Apri il menu nutrimento per calcolare gli stack", "Abra o menu de alimentação para calcular stacks", "Otwórz menu karmienia, aby obliczyć stacki", "Buka menu makan untuk mempelajari XP sumber daya", "Kaynak XP öğrenmek için besleme menüsünü aç");
        add("extras.boss_soon", "Spawns within 30 seconds", "Apparaît dans 30 secondes", "Aparece en 30 segundos", "Erscheint in 30 Sekunden", "Appare entro 30 secondi", "Aparece em 30 segundos", "Pojawi się w ciągu 30 sekund", "Muncul dalam 30 detik", "30 saniye içinde doğar");
        add("extras.filter", "Filter", "Filtrer", "Filtrar", "Filter", "Filtro", "Filtrar", "Filtr", "Filter", "Filtre");
        add("extras.clear_filter", "Clear filters", "Effacer les filtres", "Borrar filtros", "Filter löschen", "Rimuovi filtri", "Limpar filtros", "Wyczyść filtry", "Hapus filter", "Filtreleri temizle");
        add("extras.sort_rarity", "Sort storage by rarity", "Trier le coffre par rareté", "Ordenar cofre por rareza", "Kiste nach Seltenheit sortieren", "Ordina baule per rarità", "Ordenar baú por raridade", "Sortuj skrzynię według rzadkości", "Urutkan peti menurut kelangkaan", "Sandığı nadirliğe göre sırala");
        add("extras.sort_name", "Sort storage A–Z", "Trier le coffre A–Z", "Ordenar cofre A–Z", "Kiste A–Z sortieren", "Ordina baule A–Z", "Ordenar baú A–Z", "Sortuj skrzynię A–Z", "Urutkan peti A–Z", "Sandığı A–Z sırala");
        add("extras.updates", "Check GitHub for 26.2 updates", "Vérifier les mises à jour 26.2 sur GitHub", "Buscar actualizaciones 26.2 en GitHub", "GitHub auf 26.2-Updates prüfen", "Controlla aggiornamenti 26.2 su GitHub", "Verificar atualizações 26.2 no GitHub", "Sprawdź aktualizacje 26.2 na GitHub", "Periksa pembaruan 26.2 di GitHub", "GitHub 26.2 güncellemelerini denetle");
        add("extras.update_available", "Mod update available", "Mise à jour disponible", "Actualización disponible", "Mod-Update verfügbar", "Aggiornamento mod disponibile", "Atualização disponível", "Dostępna aktualizacja moda", "Pembaruan mod tersedia", "Mod güncellemesi mevcut");
        add("extras.help_menus", "Open /boss, /pets or /pass quests to update their data.", "Ouvrez /boss, /pets ou /pass quests pour actualiser.", "Abre /boss, /pets o /pass quests para actualizar.", "/boss, /pets oder /pass quests zum Aktualisieren öffnen.", "Apri /boss, /pets o /pass quests per aggiornare.", "Abra /boss, /pets ou /pass quests para atualizar.", "Otwórz /boss, /pets lub /pass quests, aby odświeżyć.", "Buka /boss, /pets atau /pass quests untuk memperbarui.", "Yenilemek için /boss, /pets veya /pass quests aç.");
        add("extras.help_layout", "Move and resize panels in the HUD editor; save them as loadouts.", "Déplacez les panneaux dans l’éditeur HUD et sauvegardez.", "Mueve los paneles en el editor HUD y guarda perfiles.", "HUD im Editor verschieben und als Layout speichern.", "Sposta i pannelli nell’editor HUD e salva i layout.", "Mova painéis no editor HUD e salve layouts.", "Przesuwaj panele w edytorze HUD i zapisuj układy.", "Pindahkan panel di editor HUD dan simpan tata letak.", "HUD düzenleyicisinde panelleri taşı ve düzen kaydet.");
        add("extras.help_pets", "Pet details cover the page read; open other pages to inspect them.", "Les familiers affichés sont ceux de la page consultée.", "Los detalles de mascotas cubren la página consultada.", "Begleiterdetails zeigen die gelesene Seite.", "I dettagli pet riguardano la pagina letta.", "Os detalhes dos mascotes cobrem a página lida.", "Szczegóły zwierzaków dotyczą odczytanej strony.", "Detail pet mencakup halaman yang dibaca.", "Pet bilgileri okunan sayfayı kapsar.");
        add("extras.help_sort", "Rarity buttons filter items; sorting moves items in vanilla storage only.", "Les filtres masquent les objets ; le tri déplace les objets des coffres vanilla.", "Los filtros atenúan objetos; ordenar mueve objetos solo en cofres vanilla.", "Filter blenden Items ab; Sortieren bewegt Items nur in Vanilla-Kisten.", "I filtri oscurano gli oggetti; l’ordine sposta gli oggetti solo nei bauli vanilla.", "Filtros escurecem itens; ordenar move itens só em baús vanilla.", "Filtry przyciemniają przedmioty; sortowanie działa w zwykłych skrzyniach.", "Filter meredupkan item; pengurutan memindahkan item di peti vanilla saja.", "Filtre eşyaları karartır; sıralama yalnızca vanilla sandıklarda taşır.");
        add("ui.icon_rows", "Item icon grids", "Grilles avec icônes", "Cuadrículas con iconos", "Item-Symbolraster", "Griglie con icone", "Grades com ícones", "Siatki z ikonami", "Kisi ikon item", "Eşya simge ızgarası");
        add("progress.level_sync_hint", "Level changed — open /profile to sync", "Niveau modifié — ouvrez /profile", "Nivel cambiado — abre /profile", "Level geändert — /profile öffnen", "Livello cambiato — apri /profile", "Nível mudou — abra /profile", "Poziom zmieniony — otwórz /profile", "Level berubah — buka /profile", "Seviye değişti — /profile aç");
        add("progress.cached_hint", "Saved XP • waiting for live level", "XP sauvegardée • attente du niveau", "XP guardada • esperando nivel", "Gespeicherte XP • warte auf Level", "XP salvata • attendo livello attuale", "XP salvo • aguardando nível", "Zapisane XP • czekam na poziom", "XP tersimpan • menunggu level", "Kayıtlı XP • seviye bekleniyor");
        add("ui.text", "Text", "Texte", "Texto", "Text", "Testo", "Texto", "Tekst", "Teks", "Yazı");
        add("menu.group.general", "General", "Général", "General", "Allgemein", "Generale", "Geral", "Ogólne", "Umum", "Genel");
        add("menu.group.hud", "HUD", "HUD", "HUD", "HUD", "HUD", "HUD", "HUD", "HUD", "HUD");
        add("menu.group.combat", "Combat", "Combat", "Combate", "Kampf", "Combattimento", "Combate", "Walka", "Pertarungan", "Savaş");
        add("menu.group.progress", "Progress", "Progression", "Progreso", "Fortschritt", "Progressi", "Progresso", "Postęp", "Kemajuan", "İlerleme");
        add("menu.group.economy", "Economy", "Économie", "Economía", "Wirtschaft", "Economia", "Economia", "Ekonomia", "Ekonomi", "Ekonomi");
        add("menu.group.tools", "Tools", "Outils", "Herramientas", "Werkzeuge", "Strumenti", "Ferramentas", "Narzędzia", "Alat", "Araçlar");
        add("ui.panels", "Panels", "Panneaux", "Paneles", "Panels", "Pannelli", "Painéis", "Panele", "Panel", "Paneller");
        add("ui.overall_scale", "Overall size", "Taille globale", "Tamaño general", "Gesamtgröße", "Dimensione totale", "Tamanho geral", "Rozmiar całości", "Ukuran keseluruhan", "Genel boyut");
        add("ui.text_size", "Text size", "Taille du texte", "Tamaño de texto", "Textgröße", "Dimensione testo", "Tamanho do texto", "Rozmiar tekstu", "Ukuran teks", "Metin boyutu");
        add("ui.box_width", "Box width", "Largeur", "Ancho", "Breite", "Larghezza", "Largura", "Szerokość", "Lebar kotak", "Kutu genişliği");
        add("ui.box_height", "Box height", "Hauteur", "Alto", "Höhe", "Altezza", "Altura", "Wysokość", "Tinggi kotak", "Kutu yüksekliği");
        add("ui.title", "Title", "Titre", "Título", "Titel", "Titolo", "Título", "Tytuł", "Judul", "Başlık");
        add("ui.default", "Default", "Par défaut", "Predeterminado", "Standard", "Predefinito", "Padrão", "Domyślnie", "Bawaan", "Varsayılan");
        add("ui.on", "On", "Activé", "Activado", "An", "Attivo", "Ligado", "Wł.", "Aktif", "Açık");
        add("ui.off", "Off", "Désactivé", "Desactivado", "Aus", "Disattivo", "Desligado", "Wył.", "Nonaktif", "Kapalı");
        add("ui.auto", "Auto", "Auto", "Auto", "Auto", "Auto", "Auto", "Auto", "Otomatis", "Otomatik");
        add("ui.panel_color", "Panel color", "Couleur", "Color del panel", "Panelfarbe", "Colore pannello", "Cor do painel", "Kolor panelu", "Warna panel", "Panel rengi");
        add("ui.hud_customization", "HUD customization", "Personnalisation HUD", "Personalizar HUD", "HUD anpassen", "Personalizza HUD", "Personalizar HUD", "Dostosuj HUD", "Sesuaikan HUD", "HUD özelleştirme");
        add("ui.selected_stats", "Stats shown", "Stats affichées", "Estadísticas visibles", "Angezeigte Werte", "Statistiche visibili", "Estatísticas visíveis", "Widoczne statystyki", "Statistik ditampilkan", "Gösterilen istatistikler");
        add("ui.reset_stats", "Reset stat selection", "Réinitialiser la sélection", "Restablecer selección", "Auswahl zurücksetzen", "Ripristina selezione", "Redefinir seleção", "Resetuj wybór", "Atur ulang pilihan", "Seçimi sıfırla");
        add("ui.pass_pets", "Pass and pets", "Pass et familiers", "Pase y mascotas", "Pass und Begleiter", "Pass e pet", "Passe e pets", "Przepustka i zwierzaki", "Pass dan peliharaan", "Pass ve evcil hayvanlar");
        add("bosses.settings", "Boss settings and Haki", "Réglages boss et Haki", "Ajustes de jefes y Haki", "Boss-Einstellungen und Haki", "Impostazioni boss e Haki", "Configurações de chefes e Haki", "Ustawienia bossów i Haki", "Pengaturan boss dan Haki", "Boss ayarları ve Haki");
        add("bosses.automatic_reads", "Automatic boss reads", "Lecture automatique des boss", "Lectura automática de jefes", "Automatische Boss-Abfragen", "Lettura automatica boss", "Leitura automática de chefes", "Automatyczny odczyt bossów", "Baca boss otomatis", "Otomatik boss okuma");
        add("bosses.order", "Boss order", "Ordre des boss", "Orden de jefes", "Boss-Reihenfolge", "Ordine boss", "Ordem dos chefes", "Kolejność bossów", "Urutan boss", "Boss sırası");
        add("bosses.by_time", "By time", "Par délai", "Por tiempo", "Nach Zeit", "Per tempo", "Por tempo", "Według czasu", "Menurut waktu", "Zamana göre");
        add("bosses.by_island", "By island", "Par île", "Por isla", "Nach Insel", "Per isola", "Por ilha", "Według wyspy", "Menurut pulau", "Adaya göre");
        add("ui.auction_house", "Auction house", "Hôtel des ventes", "Casa de subastas", "Auktionshaus", "Casa d’aste", "Casa de leilões", "Dom aukcyjny", "Rumah lelang", "Müzayede evi");
        add("ui.inventory_helpers", "Inventory helpers", "Aides d’inventaire", "Ayudas de inventario", "Inventarhilfen", "Aiuti inventario", "Auxílios de inventário", "Narzędzia ekwipunku", "Bantuan inventaris", "Envanter yardımcıları");
        add("ui.appearance", "Appearance", "Apparence", "Apariencia", "Aussehen", "Aspetto", "Aparência", "Wygląd", "Tampilan", "Görünüm");
        add("ui.loadouts", "Loadouts", "Configurations", "Perfiles", "Layouts", "Layout", "Layouts", "Układy", "Tata letak", "Düzenler");
        add("ui.pictures", "Pictures", "Images", "Imágenes", "Bilder", "Immagini", "Imagens", "Obrazy", "Gambar", "Resimler");
        add("ui.done", "Done", "Terminé", "Listo", "Fertig", "Fatto", "Pronto", "Gotowe", "Selesai", "Bitti");
        add("ui.accent", "Accent", "Accent", "Acento", "Akzent", "Accento", "Destaque", "Akcent", "Aksen", "Vurgu");
        add("ui.background", "Background", "Fond", "Fondo", "Hintergrund", "Sfondo", "Fundo", "Tło", "Latar", "Arka plan");
        add("ui.borders", "Border", "Bordure", "Borde", "Rahmen", "Bordo", "Borda", "Obramowanie", "Bingkai", "Kenarlık");
        add("ui.hex", "Hex color", "Couleur hex", "Color hex", "Hex-Farbe", "Colore hex", "Cor hex", "Kolor hex", "Warna hex", "Hex renk");
        add("ui.menu_opacity", "Menu opacity", "Opacité menu", "Opacidad menú", "Menüdeckkraft", "Opacità menu", "Opacidade menu", "Krycie menu", "Opasitas menu", "Menü opaklığı");
        add("ui.random", "Random", "Aléatoire", "Aleatorio", "Zufällig", "Casuale", "Aleatório", "Losowy", "Acak", "Rastgele");
        add("ui.hud_style", "HUD opacity & text colors", "Opacité HUD et couleurs du texte", "Opacidad HUD y colores del texto", "HUD-Deckkraft und Textfarben", "Opacità HUD e colori del testo", "Opacidade HUD e cores do texto", "Krycie HUD i kolory tekstu", "Opasitas HUD dan warna teks", "HUD opaklığı ve metin renkleri");
        add("hud.panel.clock", "Clock", "Horloge", "Reloj", "Uhr", "Orologio", "Relógio", "Zegar", "Jam", "Saat");
        add("hud.panel.chat", "Chat indicator", "Indicateur de chat", "Indicador de chat", "Chat-Anzeige", "Indicatore chat", "Indicador de chat", "Wskaźnik czatu", "Indikator chat", "Sohbet göstergesi");
        add("ui.hud_opacity", "HUD opacity", "Opacité HUD", "Opacidad HUD", "HUD-Deckkraft", "Opacità HUD", "Opacidade HUD", "Krycie HUD", "Opasitas HUD", "HUD opaklığı");
        add("ui.compact", "Compact HUD", "HUD compact", "HUD compacto", "Kompaktes HUD", "HUD compatto", "HUD compacto", "Kompaktowy HUD", "HUD ringkas", "Kompakt HUD");
        add("ui.headers", "HUD headings", "Titres HUD", "Títulos HUD", "HUD-Titel", "Titoli HUD", "Títulos HUD", "Nagłówki HUD", "Judul HUD", "HUD başlıkları");
        add("ui.decorations", "Decorations", "Décorations", "Decoraciones", "Dekorationen", "Decorazioni", "Decorações", "Ozdoby", "Dekorasi", "Süslemeler");
        add("ui.shadows", "HUD text shadow", "Ombre texte HUD", "Sombra texto HUD", "HUD-Textschatten", "Ombra testo HUD", "Sombra texto HUD", "Cień tekstu HUD", "Bayangan teks HUD", "HUD yazı gölgesi");
        add("ui.rarities", "Rarity badges", "Icônes de rareté", "Iconos de rareza", "Seltenheitssymbole", "Icone rarità", "Ícones de raridade", "Ikony rzadkości", "Ikon kelangkaan", "Nadirlik simgeleri");
        add("ui.reset_style", "Reset appearance", "Réinitialiser le style", "Restablecer estilo", "Stil zurücksetzen", "Reimposta stile", "Redefinir estilo", "Resetuj wygląd", "Reset tampilan", "Görünümü sıfırla");
        add("ui.farming", "Farming", "Récolte", "Cultivo", "Farmen", "Farming", "Coleta", "Zbieranie", "Farming", "Toplama");
        add("ui.fighting", "Fighting", "Combat", "Combate", "Kampf", "Combattimento", "Combate", "Walka", "Pertarungan", "Savaş");
        add("ui.grinding", "Grinding", "Farm intensif", "Farmeo", "Grinden", "Grinding", "Farm", "Farmienie", "Grinding", "Farm");
        add("ui.original", "Original layout", "Disposition initiale", "Diseño original", "Originales Layout", "Layout originale", "Layout original", "Oryginalny układ", "Tata letak awal", "İlk düzen");
        add("ui.applied", "Applied", "Appliqué", "Aplicado", "Angewendet", "Applicato", "Aplicado", "Zastosowano", "Diterapkan", "Uygulandı");
        add("ui.apply", "Apply", "Appliquer", "Aplicar", "Anwenden", "Applica", "Aplicar", "Zastosuj", "Terapkan", "Uygula");
        add("ui.no_loadouts", "No saved loadouts", "Aucune configuration", "Sin perfiles guardados", "Keine gespeicherten Layouts", "Nessun layout salvato", "Nenhum layout salvo", "Brak zapisanych układów", "Belum ada tata letak", "Kayıtlı düzen yok");
        add("ui.name", "Name", "Nom", "Nombre", "Name", "Nome", "Nome", "Nazwa", "Nama", "Ad");
        add("ui.my_setup", "My setup", "Ma configuration", "Mi diseño", "Mein Layout", "Il mio layout", "Meu layout", "Mój układ", "Tata letak saya", "Düzenim");
        add("ui.save_new", "Save new", "Créer", "Guardar", "Neu speichern", "Salva nuovo", "Salvar novo", "Zapisz nowy", "Simpan baru", "Yeni kaydet");
        add("ui.update", "Update", "Actualiser", "Actualizar", "Aktualisieren", "Aggiorna", "Atualizar", "Aktualizuj", "Perbarui", "Güncelle");
        add("ui.delete", "Remove", "Retirer", "Eliminar", "Entfernen", "Rimuovi", "Remover", "Usuń", "Hapus", "Kaldır");
        add("ui.name_required", "Enter a name", "Entrez un nom", "Escribe un nombre", "Namen eingeben", "Inserisci un nome", "Digite um nome", "Wpisz nazwę", "Masukkan nama", "Ad girin");
        add("ui.name_exists", "This name is already used", "Ce nom existe déjà", "Este nombre ya existe", "Name bereits vergeben", "Nome già utilizzato", "Nome já utilizado", "Nazwa już istnieje", "Nama sudah dipakai", "Bu ad kullanılıyor");
        add("ui.saved", "Saved", "Enregistré", "Guardado", "Gespeichert", "Salvato", "Salvo", "Zapisano", "Disimpan", "Kaydedildi");
        add("ui.path", "Image file path", "Chemin de l’image", "Ruta de imagen", "Bilddateipfad", "Percorso immagine", "Caminho da imagem", "Ścieżka obrazu", "Lokasi gambar", "Resim dosyası yolu");
        add("ui.import", "Import", "Importer", "Importar", "Importieren", "Importa", "Importar", "Importuj", "Impor", "İçe aktar");
        add("ui.bad_path", "Cannot open this image", "Impossible d’ouvrir l’image", "No se puede abrir la imagen", "Bild kann nicht geöffnet werden", "Impossibile aprire immagine", "Não foi possível abrir a imagem", "Nie można otworzyć obrazu", "Gambar tidak dapat dibuka", "Resim açılamadı");
        add("ui.visible", "Visible", "Visible", "Visible", "Sichtbar", "Visibile", "Visível", "Widoczny", "Terlihat", "Görünür");
        add("ui.opacity", "Opacity", "Opacité", "Opacidad", "Deckkraft", "Opacità", "Opacidade", "Krycie", "Opasitas", "Opaklık");
        add("ui.image_limit", "Up to 8 pictures per layout", "8 images par configuration", "Hasta 8 imágenes por perfil", "Bis zu 8 Bilder pro Layout", "Fino a 8 immagini per layout", "Até 8 imagens por layout", "Do 8 obrazów na układ", "Hingga 8 gambar per tata letak", "Düzen başına en fazla 8 resim");
        add("ui.imported", "Image imported", "Image importée", "Imagen importada", "Bild importiert", "Immagine importata", "Imagem importada", "Obraz zaimportowany", "Gambar diimpor", "Resim içe aktarıldı");
        add("ui.visible_panels", "Visible panels • tracking keeps running", "Panneaux visibles • suivi maintenu", "Paneles visibles • seguimiento activo", "Sichtbare Panels • Tracking läuft weiter", "Pannelli visibili • tracking sempre attivo", "Painéis visíveis • rastreamento ativo", "Widoczne panele • śledzenie nadal działa", "Panel terlihat • pelacakan tetap aktif", "Görünür paneller • takip sürer");
        add("ui.image_hint", "Drop PNG/JPG files or a folder, or paste a path", "Glissez un PNG/JPG ou un dossier, ou collez un chemin", "Suelta PNG/JPG o una carpeta, o pega una ruta", "PNG/JPG oder Ordner ablegen oder Pfad einfügen", "Trascina PNG/JPG o una cartella, o incolla un percorso", "Arraste PNG/JPG ou uma pasta, ou cole um caminho", "Upuść PNG/JPG lub folder albo wklej ścieżkę", "Seret PNG/JPG atau folder, atau tempel lokasi", "PNG/JPG veya klasör sürükleyin ya da yol yapıştırın");
        add("ui.choose_folder", "Choose folder", "Choisir un dossier", "Elegir carpeta", "Ordner wählen", "Scegli cartella", "Escolher pasta", "Wybierz folder", "Pilih folder", "Klasör seç");
        add("ui.rotation_interval", "Interval (s)", "Intervalle (s)", "Intervalo (s)", "Intervall (s)", "Intervallo (s)", "Intervalo (s)", "Odstęp (s)", "Interval (s)", "Aralık (sn)");
        add("ui.interval_range", "Enter an interval from 1 to 3600 seconds.", "Saisissez un intervalle de 1 à 3600 secondes.", "Introduce un intervalo de 1 a 3600 segundos.", "Intervall von 1 bis 3600 Sekunden eingeben.", "Inserisci un intervallo da 1 a 3600 secondi.", "Insira um intervalo de 1 a 3600 segundos.", "Wpisz odstęp od 1 do 3600 sekund.", "Masukkan interval dari 1 hingga 3600 detik.", "1 ile 3600 saniye arasında bir aralık girin.");
        add("ui.image_edit_hint", "HUD editor: drag to move • scroll to resize • H to hide", "Éditeur HUD : glisser • molette • H pour masquer", "Editor HUD: arrastrar • rueda • H para ocultar", "HUD-Editor: ziehen • scrollen • H ausblenden", "Editor HUD: trascina • rotella • H per nascondere", "Editor HUD: arraste • role • H para ocultar", "Edytor HUD: przeciągnij • przewijaj • H ukrywa", "Editor HUD: seret • gulir • H sembunyikan", "HUD: sürükle • kaydırarak boyutlandır • H gizle");
        add("hud.panel.cooking", "Cooking", "Cuisine", "Cocina", "Kochen", "Cucina", "Culinária", "Gotowanie", "Memasak", "Yemek pişirme");
        add("cooking.quantity", "Dishes (1–64)", "Plats (1–64)", "Platos (1–64)", "Gerichte (1–64)", "Piatti (1–64)", "Pratos (1–64)", "Dania (1–64)", "Hidangan (1–64)", "Yemek sayısı (1–64)");
        add("cooking.clear", "Clear recipe", "Effacer la recette", "Borrar receta", "Rezept löschen", "Rimuovi ricetta", "Limpar receita", "Wyczyść przepis", "Hapus resep", "Tarifi temizle");
        add("cooking.hover_hint", "Hover over a recipe to track ingredients", "Survolez une recette pour suivre les ingrédients", "Pasa sobre una receta para seguir ingredientes", "Rezept ansehen, um Zutaten zu verfolgen", "Passa su una ricetta per seguirne gli ingredienti", "Passe sobre uma receita para acompanhar ingredientes", "Najedź na przepis, aby śledzić składniki", "Arahkan kursor ke resep untuk melacak bahan", "Malzemeleri takip etmek için tarifin üzerine gel");
        add("tab.progress", "Progress", "Progression", "Progreso", "Fortschritt", "Progressi", "Progresso", "Postęp", "Progres", "İlerleme");
        add("hud.panel.profile_xp", "Profile XP", "XP du profil", "XP del perfil", "Profil-XP", "XP profilo", "XP do perfil", "XP profilu", "XP profil", "Profil XP");
        add("hud.panel.grinding", "Grinding", "Farm", "Farmeo", "Grinden", "Grinding", "Farm", "Farmienie", "Grinding", "Farm");
        add("progress.level", "Player level", "Niveau du joueur", "Nivel del jugador", "Spielerlevel", "Livello giocatore", "Nível do jogador", "Poziom gracza", "Level pemain", "Oyuncu seviyesi");
        add("chat_channel.public", "Public", "Public", "Público", "Öffentlich", "Pubblica", "Público", "Publiczny", "Publik", "Genel");
        add("chat_channel.party", "Party", "Groupe", "Grupo", "Gruppe", "Gruppo", "Grupo", "Grupa", "Grup", "Grup");
        add("chat_channel.island", "Island", "Île", "Isla", "Insel", "Isola", "Ilha", "Wyspa", "Pulau", "Ada");
        add("chat_channel.unknown", "Chat ?", "Chat ?", "Chat ?", "Chat ?", "Chat ?", "Chat ?", "Czat ?", "Chat ?", "Sohbet ?");
        add("progress.gained", "XP gained", "XP gagnée", "XP ganada", "XP erhalten", "XP guadagnata", "XP ganho", "Zdobyte XP", "XP diperoleh", "Kazanılan XP");
        add("progress.sync_hint", "Open /profile to sync level and XP", "Ouvrez /profile pour synchroniser les XP", "Abre /profile para sincronizar XP", "/profile öffnen, um XP abzugleichen", "Apri /profile per sincronizzare gli XP", "Abra /profile para sincronizar XP", "Otwórz /profile, aby zsynchronizować XP", "Buka /profile untuk sinkronisasi XP", "XP eşitlemek için /profile aç");
        add("progress.estimate_hint", "~ Player XP estimated from shared item XP or the XP bar", "~ XP joueur estimée depuis les objets ou la barre XP", "~ XP del jugador estimada de objetos o barra XP", "~ Spieler-XP aus Item-XP oder XP-Leiste geschätzt", "~ XP giocatore stimata dagli oggetti o dalla barra XP", "~ XP do jogador estimado de itens ou barra XP", "~ XP gracza szacowane z przedmiotów lub paska XP", "~ XP pemain diperkirakan dari item atau bilah XP", "~ Oyuncu XP eşya XP veya XP çubuğundan tahmin edilir");
        add("grinding.active", "Grinding: active", "Farm : actif", "Farmeo: activo", "Grinden: aktiv", "Grinding: attivo", "Farm: ativo", "Farmienie: aktywne", "Grinding: aktif", "Farm: aktif");
        add("grinding.paused", "Grinding: paused", "Farm : en pause", "Farmeo: pausado", "Grinden: pausiert", "Grinding: in pausa", "Farm: pausado", "Farmienie: pauza", "Grinding: dijeda", "Farm: duraklatıldı");
        add("grinding.money", "Money earned", "Argent gagné", "Dinero ganado", "Geld verdient", "Denaro guadagnato", "Dinheiro ganho", "Zarobione pieniądze", "Uang diperoleh", "Kazanılan para");
        add("grinding.money_hour", "Money/h", "Argent/h", "Dinero/h", "Geld/h", "Denaro/h", "Dinheiro/h", "Pieniądze/h", "Uang/jam", "Para/saat");
        add("grinding.time", "Active time", "Temps actif", "Tiempo activo", "Aktive Zeit", "Tempo attivo", "Tempo ativo", "Czas aktywności", "Waktu aktif", "Aktif süre");
        add("grinding.reset", "Reset grinding session", "Réinitialiser le farm", "Reiniciar farmeo", "Grind zurücksetzen", "Azzera sessione grinding", "Reiniciar farm", "Zresetuj farmienie", "Reset sesi grinding", "Farm oturumunu sıfırla");
        add("grinding.pause_hint", "Pauses after 1 minute without XP • resets at local midnight", "Pause après 1 min sans XP • remise à zéro à minuit local", "Pausa tras 1 min sin XP • reinicio a medianoche local", "Pause nach 1 Min. ohne XP • Reset um lokale Mitternacht", "Pausa dopo 1 min senza XP • reset a mezzanotte locale", "Pausa após 1 min sem XP • reinicia à meia-noite local", "Pauza po 1 min bez XP • reset o lokalnej północy", "Jeda setelah 1 menit tanpa XP • reset tengah malam lokal", "1 dk XP yoksa durur • yerel gece yarısı sıfırlanır");

        add("menu.title", "Minepiece QoL", "Minepiece QoL", "Minepiece QoL", "Minepiece QoL", "Minepiece QoL", "Minepiece QoL", "Minepiece QoL", "Minepiece QoL", "Minepiece QoL");
        add("menu.credits", "Made by SiickLukee", "Fait par SiickLukee", "Hecho por SiickLukee", "Erstellt von SiickLukee", "Creato da SiickLukee", "Feito por SiickLukee", "Stworzone przez SiickLukee", "Dibuat oleh SiickLukee", "SiickLukee tarafından yapıldı");

        add("tab.main", "Main", "Principal", "Principal", "Haupt", "Principale", "Principal", "Główne", "Utama", "Ana");
        add("tab.bosses", "Bosses", "Boss", "Jefes", "Bosse", "Boss", "Chefes", "Bossowie", "Boss", "Bosslar");
        add("tab.money", "Money", "Argent", "Dinero", "Geld", "Denaro", "Dinheiro", "Pieniądze", "Uang", "Para");
        add("tab.jobs", "Jobs", "Métiers", "Trabajos", "Berufe", "Mestieri", "Profissões", "Prace", "Pekerjaan", "Meslekler");
        add("tab.events", "Events", "Événements", "Eventos", "Events", "Eventi", "Eventos", "Wydarzenia", "Event", "Etkinlikler");
        add("tab.profile", "Profile", "Profil", "Perfil", "Profil", "Profilo", "Perfil", "Profil", "Profil", "Profil");
        add("tab.language", "Language", "Langue", "Idioma", "Sprache", "Lingua", "Idioma", "Język", "Bahasa", "Dil");
        add("tab.other", "Other", "Autres", "Otros", "Andere", "Altro", "Outros", "Inne", "Lainnya", "Diğer");

        add("setting.mod_enabled", "Mod Enabled", "Mod activé", "Mod activado", "Mod aktiviert", "Mod attivo", "Mod ativado", "Mod włączony", "Mod aktif", "Mod etkin");
        add("setting.show_all_features", "Show All HUD Features", "Afficher toutes les fonctionnalités HUD", "Mostrar todas las funciones del HUD", "Alle HUD-Funktionen anzeigen", "Mostra tutte le funzioni HUD", "Mostrar todos os recursos da HUD", "Pokaż wszystkie funkcje HUD", "Tampilkan semua fitur HUD", "Tüm HUD özelliklerini göster");
        add("setting.debug_logging", "Debug Logging", "Journal de debug", "Registro de depuración", "Debug-Protokoll", "Log di debug", "Log de depuração", "Log debugowania", "Log debug", "Hata ayıklama günlüğü");
        add("button.edit_hud_layout", "Edit HUD Layout", "Modifier la disposition HUD", "Editar diseño del HUD", "HUD-Layout bearbeiten", "Modifica layout HUD", "Editar layout da HUD", "Edytuj układ HUD", "Edit tata letak HUD", "HUD düzenini düzenle");
        add("button.reset_hud_layout", "Reset HUD Layout", "Réinitialiser la disposition HUD", "Restablecer diseño HUD", "HUD-Layout zurücksetzen", "Reimposta layout HUD", "Redefinir layout da HUD", "Resetuj układ HUD", "Reset tata letak HUD", "HUD düzenini sıfırla");
        add("button.save", "Save", "Enregistrer", "Guardar", "Speichern", "Salva", "Salvar", "Zapisz", "Simpan", "Kaydet");
        add("button.run_balance", "Run /balance", "Lancer /balance", "Ejecutar /balance", "/balance ausführen", "Esegui /balance", "Executar /balance", "Uruchom /balance", "Jalankan /balance", "/balance çalıştır");
        add("button.save_jobs", "Save Jobs", "Enregistrer métiers", "Guardar trabajos", "Berufe speichern", "Salva mestieri", "Salvar profissões", "Zapisz prace", "Simpan pekerjaan", "Meslekleri kaydet");
        add("button.add", "+ Add", "+ Ajouter", "+ Añadir", "+ Hinzufügen", "+ Aggiungi", "+ Adicionar", "+ Dodaj", "+ Tambah", "+ Ekle");
        add("button.sync_profile", "Sync /profile", "Sync /profile", "Sync /profile", "Sync /profile", "Sync /profile", "Sync /profile", "Sync /profile", "Sync /profile", "Sync /profile");
        add("button.add_rule", "+ Add Rule", "+ Ajouter règle", "+ Añadir regla", "+ Regel hinzufügen", "+ Aggiungi regola", "+ Adicionar regra", "+ Dodaj regułę", "+ Tambah aturan", "+ Kural ekle");
        add("button.on", "On", "On", "On", "An", "On", "On", "Wł.", "On", "Açık");
        add("button.off", "Off", "Off", "Off", "Aus", "Off", "Off", "Wył.", "Off", "Kapalı");
        add("main.reopen_hint", "Press . to reopen this menu (rebind in Controls)", "Appuyez sur . pour rouvrir ce menu (modifiable dans Contrôles)", "Pulsa . para reabrir este menú (reasignable en Controles)", "Drücke . um dieses Menü erneut zu öffnen (in Steuerung anpassbar)", "Premi . per riaprire questo menu (rimappabile nei Controlli)", "Pressione . para reabrir este menu (redefina em Controles)", "Naciśnij . aby ponownie otworzyć to menu (zmień w Sterowaniu)", "Tekan . untuk membuka kembali menu ini (ubah di Kontrol)", "Bu menüyü yeniden açmak için . tuşuna basın (Kontroller'dan değiştirin)");
        add("startup.open_menu_hint", "Open MinePiece Mod with %s.", "Ouvrez MinePiece Mod avec %s.", "Abre MinePiece Mod con %s.", "Öffne MinePiece Mod mit %s.", "Apri MinePiece Mod con %s.", "Abra o MinePiece Mod com %s.", "Otwórz MinePiece Mod klawiszem %s.", "Buka MinePiece Mod dengan %s.", "MinePiece Mod'u %s ile aç.");
        add("main.panel_colors", "Panel Colors", "Couleurs des panneaux", "Colores de panel", "Panelfarben", "Colori pannello", "Cores dos painéis", "Kolory paneli", "Warna panel", "Panel renkleri");
        add("main.mod_language", "Mod Language", "Langue du mod", "Idioma del mod", "Mod-Sprache", "Lingua del mod", "Idioma do mod", "Język moda", "Bahasa mod", "Mod dili");
        add("main.reset_hud_confirm_title", "Reset HUD Layout?", "Réinitialiser la disposition HUD ?", "¿Restablecer diseño HUD?", "HUD-Layout zurücksetzen?", "Reimpostare layout HUD?", "Redefinir layout da HUD?", "Zresetować układ HUD?", "Reset tata letak HUD?", "HUD düzeni sıfırlansın mı?");
        add("main.reset_hud_confirm_body", "This will restore default panel positions and sizes.", "Cela restaurera les positions et tailles par défaut des panneaux.", "Esto restaurará las posiciones y tamaños predeterminados de los paneles.", "Dies stellt die Standardpositionen und -größen der Panels wieder her.", "Questo ripristinerà posizioni e dimensioni predefinite dei pannelli.", "Isso restaurará as posições e tamanhos padrão dos painéis.", "To przywróci domyślne pozycje i rozmiary paneli.", "Ini akan mengembalikan posisi dan ukuran panel default.", "Bu işlem panel konum ve boyutlarını varsayılanlara döndürür.");

        add("setting.boss_tracking", "Boss Tracking", "Suivi des boss", "Seguimiento de jefes", "Boss-Tracking", "Tracciamento boss", "Rastreamento de chefes", "Śledzenie bossów", "Pelacakan boss", "Boss takibi");
        add("setting.miniboss_waypoints", "Miniboss Waypoints", "Waypoints miniboss", "Waypoints de miniboss", "Miniboss-Wegpunkte", "Waypoint miniboss", "Waypoints de miniboss", "Punkty minimbossów", "Waypoint miniboss", "Miniboss işaretleri");
        add("setting.miniboss_panel", "Miniboss Panel", "Panneau miniboss", "Panel de miniboss", "Miniboss-Panel", "Pannello miniboss", "Painel de miniboss", "Panel minimbossów", "Panel miniboss", "Miniboss paneli");
        add("bosses.tracked_by_spawn", "Tracked (by spawn)", "Suivi (par spawn)", "Seguidos (por spawn)", "Verfolgt (nach Spawn)", "Tracciati (per spawn)", "Rastreados (por spawn)", "Śledzone (wg spawnu)", "Dilacak (berdasarkan spawn)", "Takip (spawn'a göre)");
        add("bosses.registry", "Boss Registry", "Registre des boss", "Registro de jefes", "Boss-Register", "Registro boss", "Registro de chefes", "Rejestr bossów", "Daftar boss", "Boss kaydı");
        add("bosses.spawn", "Spawn", "Spawn", "Spawn", "Spawn", "Spawn", "Spawn", "Spawn", "Spawn", "Spawn");
        add("bosses.current", "current", "actuel", "actual", "aktuell", "attuale", "atual", "obecny", "saat ini", "güncel");
        add("bosses.none", "(none)", "(aucun)", "(ninguno)", "(keine)", "(nessuno)", "(nenhum)", "(brak)", "(tidak ada)", "(yok)");
        add("bosses.empty_registry", "(empty - encounter a boss to register it)", "(vide - rencontrez un boss pour l'enregistrer)", "(vacío - encuentra un jefe para registrarlo)", "(leer - begegne einem Boss, um ihn zu registrieren)", "(vuoto - incontra un boss per registrarlo)", "(vazio - encontre um chefe para registrá-lo)", "(puste - spotkaj bossa, aby go dodać)", "(kosong - temui boss untuk mendaftarkannya)", "(boş - kaydetmek için bir boss ile karşılaşın)");
        add("bosses.none_tracked", "(none tracked)", "(aucun suivi)", "(ninguno seguido)", "(nichts verfolgt)", "(nessuno tracciato)", "(nenhum rastreado)", "(nic nie śledzone)", "(tidak ada yang dilacak)", "(takip edilen yok)");
        add("bosses.minibosses", "Minibosses", "Miniboss", "Minibosses", "Minibosse", "Miniboss", "Minibosses", "Minibossy", "Miniboss", "Minibosslar");

        add("setting.money_tracking", "Money Tracking", "Suivi de l'argent", "Seguimiento de dinero", "Geld-Tracking", "Tracciamento denaro", "Rastreamento de dinheiro", "Śledzenie pieniędzy", "Pelacakan uang", "Para takibi");
        add("money.total", "Total:", "Total :", "Total:", "Gesamt:", "Totale:", "Total:", "Łącznie:", "Total:", "Toplam:");
        add("money.made_today", "Made today:", "Gagné aujourd'hui :", "Ganado hoy:", "Heute verdient:", "Guadagnato oggi:", "Ganho hoje:", "Zarobiono dziś:", "Diperoleh hari ini:", "Bugün kazanılan:");
        add("money.ah_sold", "AH sold:", "AH vendu :", "AH vendido:", "AH verkauft:", "AH venduto:", "AH vendido:", "AH sprzedane:", "AH terjual:", "AH satılan:");
        add("money.ah_bought", "AH bought:", "AH acheté :", "AH comprado:", "AH gekauft:", "AH comprato:", "AH comprado:", "AH kupione:", "AH dibeli:", "AH alınan:");
        add("money.receipts_latest", "Receipts (latest):", "Reçus (récents) :", "Recibos (más recientes):", "Belege (neueste):", "Ricevute (recenti):", "Recibos (mais recentes):", "Transakcje (najnowsze):", "Riwayat (terbaru):", "Kayıtlar (en yeni):");
        add("money.no_receipts", "(no receipts)", "(aucun reçu)", "(sin recibos)", "(keine Belege)", "(nessuna ricevuta)", "(sem recibos)", "(brak wpisów)", "(tidak ada riwayat)", "(kayıt yok)");

        add("setting.jobs_stats", "Jobs Stats", "Stats métiers", "Estadísticas de trabajos", "Berufsstatistiken", "Statistiche mestieri", "Estatísticas de profissões", "Statystyki prac", "Statistik pekerjaan", "Meslek istatistikleri");
        add("jobs.job", "Job", "Métier", "Trabajo", "Beruf", "Mestiere", "Profissão", "Praca", "Pekerjaan", "Meslek");
        add("jobs.level", "LVL", "NIV", "NIV", "LVL", "LVL", "NVL", "POZ", "LVL", "SVY");
        add("jobs.cur_xp", "Cur XP", "XP act.", "XP act.", "Akt XP", "XP att.", "XP atual", "Akt XP", "XP saat ini", "Anlık XP");
        add("jobs.need_xp", "Need XP", "XP req.", "XP req.", "Benöt XP", "XP req.", "XP necessária", "Potrz XP", "Butuh XP", "Gerekli XP");
        add("jobs.today_xp", "Today XP:", "XP aujourd'hui :", "XP de hoy:", "XP heute:", "XP oggi:", "XP de hoje:", "Dzisiejsze XP:", "XP hari ini:", "Bugünkü XP:");
        add("jobs.today_money", "Today money:", "Argent aujourd'hui :", "Dinero de hoy:", "Geld heute:", "Denaro oggi:", "Dinheiro de hoje:", "Dzisiejsze pieniądze:", "Uang hari ini:", "Bugünkü para:");
        add("jobs.hud_job", "Job:", "Métier :", "Trabajo:", "Beruf:", "Mestiere:", "Profissão:", "Praca:", "Pekerjaan:", "Meslek:");
        add("jobs.hud_xp", "XP:", "XP :", "XP:", "XP:", "XP:", "XP:", "XP:", "XP:", "XP:");
        add("jobs.hud_tick", "Tick: +%.2f money / +%.2f XP", "Tick : +%.2f argent / +%.2f XP", "Tick: +%.2f dinero / +%.2f XP", "Tick: +%.2f Geld / +%.2f XP", "Tick: +%.2f denaro / +%.2f XP", "Tick: +%.2f dinheiro / +%.2f XP", "Tick: +%.2f pieniędzy / +%.2f XP", "Tick: +%.2f uang / +%.2f XP", "Tick: +%.2f para / +%.2f XP");
        add("jobs.name.fisherman", "Fisherman", "Pêcheur", "Pescador", "Fischer", "Pescatore", "Pescador", "Rybak", "Nelayan", "Balıkçı");
        add("jobs.name.farmer", "Farmer", "Fermier", "Granjero", "Bauer", "Contadino", "Fazendeiro", "Rolnik", "Petani", "Çiftçi");
        add("jobs.name.miner", "Miner", "Mineur", "Minero", "Bergmann", "Minatore", "Mineiro", "Górnik", "Penambang", "Madenci");
        add("jobs.name.lumberjack", "Lumberjack", "Bûcheron", "Leñador", "Holzfäller", "Boscaiolo", "Lenhador", "Drwal", "Penebang", "Oduncu");

        add("setting.events_enabled", "Events Enabled", "Événements activés", "Eventos activados", "Events aktiviert", "Eventi attivi", "Eventos ativados", "Wydarzenia włączone", "Event aktif", "Etkinlikler açık");
        add("events.name_placeholder", "Event name", "Nom de l'événement", "Nombre del evento", "Eventname", "Nome evento", "Nome do evento", "Nazwa wydarzenia", "Nama event", "Etkinlik adı");
        add("events.existing", "Existing events:", "Événements existants :", "Eventos existentes:", "Vorhandene Events:", "Event esistenti:", "Eventos existentes:", "Istniejące wydarzenia:", "Event yang ada:", "Mevcut etkinlikler:");
        add("events.next_line", "Next: %s (%d:%02d:%02d)", "Prochain : %s (%d:%02d:%02d)", "Siguiente: %s (%d:%02d:%02d)", "Nächstes: %s (%d:%02d:%02d)", "Prossimo: %s (%d:%02d:%02d)", "Próximo: %s (%d:%02d:%02d)", "Następne: %s (%d:%02d:%02d)", "Berikutnya: %s (%d:%02d:%02d)", "Sonraki: %s (%d:%02d:%02d)");

        add("setting.profile_stats", "Profile Stats", "Stats du profil", "Estadísticas del perfil", "Profilwerte", "Statistiche profilo", "Estatísticas do perfil", "Statystyki profilu", "Statistik profil", "Profil istatistikleri");
        add("profile.health", "Health", "Santé", "Salud", "Gesundheit", "Salute", "Vida", "Zdrowie", "Health", "Can");
        add("profile.power", "Power", "Puissance", "Poder", "Kraft", "Potenza", "Poder", "Moc", "Power", "Güç");
        add("profile.strength", "Strength", "Force", "Fuerza", "Stärke", "Forza", "Força", "Siła", "Strength", "Kuvvet");
        add("profile.damage", "Damage", "Dégâts", "Daño", "Schaden", "Danno", "Dano", "Obrażenia", "Damage", "Hasar");
        add("profile.critical_chance", "Critical Chance", "Chance critique", "Prob. crítica", "Kritische Chance", "Prob. critica", "Chance crítica", "Szansa krytyczna", "Critical Chance", "Kritik şansı");
        add("profile.critical_damage", "Critical Damage", "Dégâts critiques", "Daño crítico", "Kritischer Schaden", "Danno critico", "Dano crítico", "Obrażenia krytyczne", "Critical Damage", "Kritik hasarı");
        add("profile.energy", "Energy", "Énergie", "Energía", "Energie", "Energia", "Energia", "Energia", "Energy", "Enerji");
        add("profile.energy_regeneration", "Energy Regeneration", "Régénération d'énergie", "Regeneración de energía", "Energie-Regeneration", "Rigenerazione energia", "Regeneração de energia", "Regeneracja energii", "Energy Regeneration", "Enerji yenilenmesi");
        add("profile.speed", "Speed", "Vitesse", "Velocidad", "Tempo", "Velocità", "Velocidade", "Szybkość", "Speed", "Hız");
        add("profile.dexterity", "Dexterity", "Dextérité", "Destreza", "Geschicklichkeit", "Destrezza", "Destreza", "Zręczność", "Dexterity", "Çeviklik");
        add("profile.defense", "Defense", "Défense", "Defensa", "Verteidigung", "Difesa", "Defesa", "Obrona", "Defense", "Savunma");
        add("profile.regeneration", "Regeneration", "Régénération", "Regeneración", "Regeneration", "Rigenerazione", "Regeneração", "Regeneracja", "Regeneration", "Yenilenme");

        add("setting.chat_translate", "Auto Chat Translation", "Traduction auto du chat", "Traducción automática del chat", "Automatische Chat-Übersetzung", "Traduzione automatica chat", "Tradução automática do chat", "Automatyczne tłumaczenie czatu", "Terjemahan chat otomatis", "Otomatik sohbet çevirisi");
        add("setting.translate_public", "Translate Public Chat", "Traduire le chat public", "Traducir chat público", "Öffentlichen Chat übersetzen", "Traduci chat pubblica", "Traduzir chat público", "Tłumacz czat publiczny", "Terjemahkan chat publik", "Genel sohbeti çevir");
        add("setting.translate_private", "Translate Private Chat", "Traduire les messages privés", "Traducir chat privado", "Privaten Chat übersetzen", "Traduci chat privata", "Traduzir chat privado", "Tłumacz czat prywatny", "Terjemahkan chat privat", "Özel sohbeti çevir");
        add("setting.translate_system", "Translate System Lines", "Traduire les lignes système", "Traducir líneas del sistema", "Systemzeilen übersetzen", "Traduci righe di sistema", "Traduzir linhas do sistema", "Tłumacz linie systemowe", "Terjemahkan baris sistem", "Sistem satırlarını çevir");
        add("setting.translate_aggressive", "Aggressive", "Aggressive", "Aggressive", "Aggressive", "Aggressive", "Aggressive", "Aggressive", "Aggressive", "Aggressive");
        add("language.from", "From:", "De :", "De:", "Von:", "Da:", "De:", "Z:", "Dari:", "Kaynak:");
        add("language.to", "To:", "Vers :", "A:", "Nach:", "A:", "Para:", "Do:", "Ke:", "Hedef:");
        add("language.active_rules", "Active rules:", "Règles actives :", "Reglas activas:", "Aktive Regeln:", "Regole attive:", "Regras ativas:", "Aktywne reguły:", "Aturan aktif:", "Aktif kurallar:");
        add("language.none_yet", "(none yet)", "(aucune)", "(aún ninguna)", "(noch keine)", "(nessuna)", "(ainda nenhuma)", "(jeszcze brak)", "(belum ada)", "(henüz yok)");
        add("language.no_languages", "No languages available.", "Aucune langue disponible.", "No hay idiomas disponibles.", "Keine Sprachen verfügbar.", "Nessuna lingua disponibile.", "Nenhum idioma disponível.", "Brak dostępnych języków.", "Tidak ada bahasa tersedia.", "Dil mevcut değil.");
        add("language.routes_disabled_warning", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.", "All translation routes are OFF. Translation was auto-disabled until at least one route is enabled.");

        add("cmd.common.none_value", "<none>", "<none>", "<none>", "<none>", "<none>", "<none>", "<none>", "<none>", "<none>");
        add("cmd.common.empty_value", "<empty>", "<empty>", "<empty>", "<empty>", "<empty>", "<empty>", "<empty>", "<empty>", "<empty>");
        add("cmd.debug.no_line", "No debug line available.", "No debug line available.", "No debug line available.", "No debug line available.", "No debug line available.", "No debug line available.", "No debug line available.", "No debug line available.", "No debug line available.");
        add("cmd.debug.copied", "Copied last debug line to clipboard.", "Copied last debug line to clipboard.", "Copied last debug line to clipboard.", "Copied last debug line to clipboard.", "Copied last debug line to clipboard.", "Copied last debug line to clipboard.", "Copied last debug line to clipboard.", "Copied last debug line to clipboard.", "Copied last debug line to clipboard.");

        add("cmd.translate.routes", "Routes: public=%s private=%s system=%s", "Routes: public=%s private=%s system=%s", "Routes: public=%s private=%s system=%s", "Routes: public=%s private=%s system=%s", "Routes: public=%s private=%s system=%s", "Routes: public=%s private=%s system=%s", "Routes: public=%s private=%s system=%s", "Routes: public=%s private=%s system=%s", "Routes: public=%s private=%s system=%s");
        add("cmd.translate.master", "Master translation: %s", "Master translation: %s", "Master translation: %s", "Master translation: %s", "Master translation: %s", "Master translation: %s", "Master translation: %s", "Master translation: %s", "Master translation: %s");
        add("cmd.translate.aggressive", "Aggressive mode: %s", "Aggressive mode: %s", "Aggressive mode: %s", "Aggressive mode: %s", "Aggressive mode: %s", "Aggressive mode: %s", "Aggressive mode: %s", "Aggressive mode: %s", "Aggressive mode: %s");
        add("cmd.translate.checklist.title", "Classification checklist:", "Classification checklist:", "Classification checklist:", "Classification checklist:", "Classification checklist:", "Classification checklist:", "Classification checklist:", "Classification checklist:", "Classification checklist:");
        add("cmd.translate.checklist.public", "- Public chat usually has a sender prefix and no private markers.", "- Public chat usually has a sender prefix and no private markers.", "- Public chat usually has a sender prefix and no private markers.", "- Public chat usually has a sender prefix and no private markers.", "- Public chat usually has a sender prefix and no private markers.", "- Public chat usually has a sender prefix and no private markers.", "- Public chat usually has a sender prefix and no private markers.", "- Public chat usually has a sender prefix and no private markers.", "- Public chat usually has a sender prefix and no private markers.");
        add("cmd.translate.checklist.private", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.", "- Private chat usually contains PM/MSG/whisper/tell/to/from markers.");
        add("cmd.translate.checklist.system", "- System lines usually have no sender divider (›/»/>).", "- System lines usually have no sender divider (›/»/>).", "- System lines usually have no sender divider (›/»/>).", "- System lines usually have no sender divider (›/»/>).", "- System lines usually have no sender divider (›/»/>).", "- System lines usually have no sender divider (›/»/>).", "- System lines usually have no sender divider (›/»/>).", "- System lines usually have no sender divider (›/»/>).", "- System lines usually have no sender divider (›/»/>).");
        add("cmd.translate.checklist.classify_hint", "Use /mptranslate classify <line> to inspect one raw line.", "Use /mptranslate classify <line> to inspect one raw line.", "Use /mptranslate classify <line> to inspect one raw line.", "Use /mptranslate classify <line> to inspect one raw line.", "Use /mptranslate classify <line> to inspect one raw line.", "Use /mptranslate classify <line> to inspect one raw line.", "Use /mptranslate classify <line> to inspect one raw line.", "Use /mptranslate classify <line> to inspect one raw line.", "Use /mptranslate classify <line> to inspect one raw line.");
        add("cmd.translate.classify_type", "Type: %s", "Type: %s", "Type: %s", "Type: %s", "Type: %s", "Type: %s", "Type: %s", "Type: %s", "Type: %s");
        add("cmd.translate.classify_divider", "Divider: %s", "Divider: %s", "Divider: %s", "Divider: %s", "Divider: %s", "Divider: %s", "Divider: %s", "Divider: %s", "Divider: %s");
        add("cmd.translate.classify_author", "Author prefix: %s", "Author prefix: %s", "Author prefix: %s", "Author prefix: %s", "Author prefix: %s", "Author prefix: %s", "Author prefix: %s", "Author prefix: %s", "Author prefix: %s");
        add("cmd.translate.classify_body", "Message body: %s", "Message body: %s", "Message body: %s", "Message body: %s", "Message body: %s", "Message body: %s", "Message body: %s", "Message body: %s", "Message body: %s");
        add("cmd.translate.classify_route_enabled", "Route enabled for this type: %s", "Route enabled for this type: %s", "Route enabled for this type: %s", "Route enabled for this type: %s", "Route enabled for this type: %s", "Route enabled for this type: %s", "Route enabled for this type: %s", "Route enabled for this type: %s", "Route enabled for this type: %s");
        add("cmd.translate.classify_invalid_reason", "Invalid parse reason: %s", "Invalid parse reason: %s", "Invalid parse reason: %s", "Invalid parse reason: %s", "Invalid parse reason: %s", "Invalid parse reason: %s", "Invalid parse reason: %s", "Invalid parse reason: %s", "Invalid parse reason: %s");

        add("cmd.mod.all_features_shown", "All mod features shown.", "All mod features shown.", "All mod features shown.", "All mod features shown.", "All mod features shown.", "All mod features shown.", "All mod features shown.", "All mod features shown.", "All mod features shown.");
        add("cmd.mod.all_features_hidden", "All mod features hidden.", "All mod features hidden.", "All mod features hidden.", "All mod features hidden.", "All mod features hidden.", "All mod features hidden.", "All mod features hidden.", "All mod features hidden.", "All mod features hidden.");
        add("cmd.jobs.table_shown", "Jobs table shown.", "Jobs table shown.", "Jobs table shown.", "Jobs table shown.", "Jobs table shown.", "Jobs table shown.", "Jobs table shown.", "Jobs table shown.", "Jobs table shown.");
        add("cmd.jobs.table_hidden", "Jobs table hidden.", "Jobs table hidden.", "Jobs table hidden.", "Jobs table hidden.", "Jobs table hidden.", "Jobs table hidden.", "Jobs table hidden.", "Jobs table hidden.", "Jobs table hidden.");
        add("cmd.jobs.no_jobs_initialized", "No jobs initialized.", "No jobs initialized.", "No jobs initialized.", "No jobs initialized.", "No jobs initialized.", "No jobs initialized.", "No jobs initialized.", "No jobs initialized.", "No jobs initialized.");
        add("cmd.jobs.init_none_loaded", "No jobs loaded.", "No jobs loaded.", "No jobs loaded.", "No jobs loaded.", "No jobs loaded.", "No jobs loaded.", "No jobs loaded.", "No jobs loaded.", "No jobs loaded.");
        add("cmd.jobs.init_loaded", "Loaded %d job baselines.", "Loaded %d job baselines.", "Loaded %d job baselines.", "Loaded %d job baselines.", "Loaded %d job baselines.", "Loaded %d job baselines.", "Loaded %d job baselines.", "Loaded %d job baselines.", "Loaded %d job baselines.");
        add("cmd.jobs.reset_daily_all", "Reset daily money and daily XP for all jobs.", "Reset daily money and daily XP for all jobs.", "Reset daily money and daily XP for all jobs.", "Reset daily money and daily XP for all jobs.", "Reset daily money and daily XP for all jobs.", "Reset daily money and daily XP for all jobs.", "Reset daily money and daily XP for all jobs.", "Reset daily money and daily XP for all jobs.", "Reset daily money and daily XP for all jobs.");
        add("cmd.jobs.unknown_job", "Unknown job. Use fisherman, farmer, miner, or lumberjack.", "Unknown job. Use fisherman, farmer, miner, or lumberjack.", "Unknown job. Use fisherman, farmer, miner, or lumberjack.", "Unknown job. Use fisherman, farmer, miner, or lumberjack.", "Unknown job. Use fisherman, farmer, miner, or lumberjack.", "Unknown job. Use fisherman, farmer, miner, or lumberjack.", "Unknown job. Use fisherman, farmer, miner, or lumberjack.", "Unknown job. Use fisherman, farmer, miner, or lumberjack.", "Unknown job. Use fisherman, farmer, miner, or lumberjack.");
        add("cmd.jobs.reset_daily_job", "Reset daily XP for %s.", "Reset daily XP for %s.", "Reset daily XP for %s.", "Reset daily XP for %s.", "Reset daily XP for %s.", "Reset daily XP for %s.", "Reset daily XP for %s.", "Reset daily XP for %s.", "Reset daily XP for %s.");
        add("cmd.jobs.invalid_values", "Invalid values. Level must be 0+ and needed XP greater than 0 and at most 1000000000.", "Valeurs invalides. Niveau : 0 ou plus ; XP requise : plus de 0, maximum 1000000000.", "Valores inválidos. Nivel: 0 o más; XP necesaria: mayor que 0 y hasta 1000000000.", "Ungültige Werte. Level: mindestens 0; benötigte XP: größer als 0, höchstens 1000000000.", "Valori non validi. Livello: almeno 0; XP richiesta: maggiore di 0, massimo 1000000000.", "Valores inválidos. Nível: 0 ou mais; XP necessário: maior que 0 e até 1000000000.", "Nieprawidłowe wartości. Poziom: co najmniej 0; wymagane XP: powyżej 0, maksymalnie 1000000000.", "Nilai tidak valid. Level minimal 0; kebutuhan XP lebih dari 0, maksimal 1000000000.", "Geçersiz değerler. Seviye en az 0; gereken XP 0’dan büyük, en fazla 1000000000.");
        add("cmd.jobs.set_baseline", "Set %s baseline to LVL %d %s XP.", "Set %s baseline to LVL %d %s XP.", "Set %s baseline to LVL %d %s XP.", "Set %s baseline to LVL %d %s XP.", "Set %s baseline to LVL %d %s XP.", "Set %s baseline to LVL %d %s XP.", "Set %s baseline to LVL %d %s XP.", "Set %s baseline to LVL %d %s XP.", "Set %s baseline to LVL %d %s XP.");
        add("cmd.jobs.debug_summary", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f");
        add("cmd.jobs.reset_daily_prefix", "Reset ", "Reset ", "Reset ", "Reset ", "Reset ", "Reset ", "Reset ", "Reset ", "Reset ");
        add("cmd.jobs.progress_reset", "Job progress reset.", "Job progress reset.", "Job progress reset.", "Job progress reset.", "Job progress reset.", "Job progress reset.", "Job progress reset.", "Job progress reset.", "Job progress reset.");
        add("cmd.jobs.baselines_cleared", "All job baselines cleared.", "All job baselines cleared.", "All job baselines cleared.", "All job baselines cleared.", "All job baselines cleared.", "All job baselines cleared.", "All job baselines cleared.", "All job baselines cleared.", "All job baselines cleared.");

        add("cmd.bosses.tracking_enabled", "Boss tracking enabled.", "Boss tracking enabled.", "Boss tracking enabled.", "Boss tracking enabled.", "Boss tracking enabled.", "Boss tracking enabled.", "Boss tracking enabled.", "Boss tracking enabled.", "Boss tracking enabled.");
        add("cmd.bosses.tracking_disabled", "Boss tracking disabled.", "Boss tracking disabled.", "Boss tracking disabled.", "Boss tracking disabled.", "Boss tracking disabled.", "Boss tracking disabled.", "Boss tracking disabled.", "Boss tracking disabled.", "Boss tracking disabled.");
        add("cmd.bosses.miniboss_registration", "Miniboss registration: %s", "Miniboss registration: %s", "Miniboss registration: %s", "Miniboss registration: %s", "Miniboss registration: %s", "Miniboss registration: %s", "Miniboss registration: %s", "Miniboss registration: %s", "Miniboss registration: %s");
        add("cmd.bosses.current_spawn", "Current spawn: %s", "Current spawn: %s", "Current spawn: %s", "Current spawn: %s", "Current spawn: %s", "Current spawn: %s", "Current spawn: %s", "Current spawn: %s", "Current spawn: %s");
        add("cmd.bosses.none_recorded", "No bosses recorded.", "No bosses recorded.", "No bosses recorded.", "No bosses recorded.", "No bosses recorded.", "No bosses recorded.", "No bosses recorded.", "No bosses recorded.", "No bosses recorded.");
        add("cmd.bosses.unnamed", "<unnamed>", "<unnamed>", "<unnamed>", "<unnamed>", "<unnamed>", "<unnamed>", "<unnamed>", "<unnamed>", "<unnamed>");
        add("cmd.bosses.cleared", "Cleared saved bosses.", "Cleared saved bosses.", "Cleared saved bosses.", "Cleared saved bosses.", "Cleared saved bosses.", "Cleared saved bosses.", "Cleared saved bosses.", "Cleared saved bosses.", "Cleared saved bosses.");
        add("cmd.bosses.cleared_index", "Cleared boss #%d.", "Cleared boss #%d.", "Cleared boss #%d.", "Cleared boss #%d.", "Cleared boss #%d.", "Cleared boss #%d.", "Cleared boss #%d.", "Cleared boss #%d.", "Cleared boss #%d.");
        add("cmd.bosses.no_saved_index", "No saved boss at index %d.", "No saved boss at index %d.", "No saved boss at index %d.", "No saved boss at index %d.", "No saved boss at index %d.", "No saved boss at index %d.", "No saved boss at index %d.", "No saved boss at index %d.", "No saved boss at index %d.");

        add("cmd.events.usage_add", "Usage: /mpevents add <event name> <HH:mm> (CET)", "Usage: /mpevents add <event name> <HH:mm> (CET)", "Usage: /mpevents add <event name> <HH:mm> (CET)", "Usage: /mpevents add <event name> <HH:mm> (CET)", "Usage: /mpevents add <event name> <HH:mm> (CET)", "Usage: /mpevents add <event name> <HH:mm> (CET)", "Usage: /mpevents add <event name> <HH:mm> (CET)", "Usage: /mpevents add <event name> <HH:mm> (CET)", "Usage: /mpevents add <event name> <HH:mm> (CET)");
        add("cmd.events.usage_remove", "Usage: /mpevents remove <event name> <HH:mm> (CET)", "Usage: /mpevents remove <event name> <HH:mm> (CET)", "Usage: /mpevents remove <event name> <HH:mm> (CET)", "Usage: /mpevents remove <event name> <HH:mm> (CET)", "Usage: /mpevents remove <event name> <HH:mm> (CET)", "Usage: /mpevents remove <event name> <HH:mm> (CET)", "Usage: /mpevents remove <event name> <HH:mm> (CET)", "Usage: /mpevents remove <event name> <HH:mm> (CET)", "Usage: /mpevents remove <event name> <HH:mm> (CET)");
        add("cmd.events.name_required", "Event name is required.", "Event name is required.", "Event name is required.", "Event name is required.", "Event name is required.", "Event name is required.", "Event name is required.", "Event name is required.", "Event name is required.");
        add("cmd.events.invalid_time", "Invalid time. Use CET format HH:mm (e.g. 19:30).", "Invalid time. Use CET format HH:mm (e.g. 19:30).", "Invalid time. Use CET format HH:mm (e.g. 19:30).", "Invalid time. Use CET format HH:mm (e.g. 19:30).", "Invalid time. Use CET format HH:mm (e.g. 19:30).", "Invalid time. Use CET format HH:mm (e.g. 19:30).", "Invalid time. Use CET format HH:mm (e.g. 19:30).", "Invalid time. Use CET format HH:mm (e.g. 19:30).", "Invalid time. Use CET format HH:mm (e.g. 19:30).");
        add("cmd.events.already_exists", "Event already exists.", "Event already exists.", "Event already exists.", "Event already exists.", "Event already exists.", "Event already exists.", "Event already exists.", "Event already exists.", "Event already exists.");
        add("cmd.events.added", "Added event: %s at %s CET.", "Added event: %s at %s CET.", "Added event: %s at %s CET.", "Added event: %s at %s CET.", "Added event: %s at %s CET.", "Added event: %s at %s CET.", "Added event: %s at %s CET.", "Added event: %s at %s CET.", "Added event: %s at %s CET.");
        add("cmd.events.not_found_time", "No matching event found for %s at %s CET.", "No matching event found for %s at %s CET.", "No matching event found for %s at %s CET.", "No matching event found for %s at %s CET.", "No matching event found for %s at %s CET.", "No matching event found for %s at %s CET.", "No matching event found for %s at %s CET.", "No matching event found for %s at %s CET.", "No matching event found for %s at %s CET.");
        add("cmd.events.removed", "Removed event: %s at %s CET.", "Removed event: %s at %s CET.", "Removed event: %s at %s CET.", "Removed event: %s at %s CET.", "Removed event: %s at %s CET.", "Removed event: %s at %s CET.", "Removed event: %s at %s CET.", "Removed event: %s at %s CET.", "Removed event: %s at %s CET.");
        add("cmd.events.invalid_time_short", "Invalid time format. Use HH:mm.", "Invalid time format. Use HH:mm.", "Invalid time format. Use HH:mm.", "Invalid time format. Use HH:mm.", "Invalid time format. Use HH:mm.", "Invalid time format. Use HH:mm.", "Invalid time format. Use HH:mm.", "Invalid time format. Use HH:mm.", "Invalid time format. Use HH:mm.");
        add("cmd.events.updated", "Updated event.", "Updated event.", "Updated event.", "Updated event.", "Updated event.", "Updated event.", "Updated event.", "Updated event.", "Updated event.");
        add("cmd.events.not_found", "Event not found.", "Event not found.", "Event not found.", "Event not found.", "Event not found.", "Event not found.", "Event not found.", "Event not found.", "Event not found.");
        add("cmd.events.none_configured", "No events configured.", "No events configured.", "No events configured.", "No events configured.", "No events configured.", "No events configured.", "No events configured.", "No events configured.", "No events configured.");
        add("cmd.events.header", "Events (CET):", "Events (CET):", "Events (CET):", "Events (CET):", "Events (CET):", "Events (CET):", "Events (CET):", "Events (CET):", "Events (CET):");
        add("cmd.events.added_prefix", "Added ", "Added ", "Added ", "Added ", "Added ", "Added ", "Added ", "Added ", "Added ");
        add("cmd.events.removed_prefix", "Removed ", "Removed ", "Removed ", "Removed ", "Removed ", "Removed ", "Removed ", "Removed ", "Removed ");

        add("cmd.money.balance_hint", "Run /balance to initialize total.", "Run /balance to initialize total.", "Run /balance to initialize total.", "Run /balance to initialize total.", "Run /balance to initialize total.", "Run /balance to initialize total.", "Run /balance to initialize total.", "Run /balance to initialize total.", "Run /balance to initialize total.");
        add("cmd.money.log_entry", "[%s] %s%s 实 - %s", "[%s] %s%s 实 - %s", "[%s] %s%s 实 - %s", "[%s] %s%s 实 - %s", "[%s] %s%s 实 - %s", "[%s] %s%s 实 - %s", "[%s] %s%s 实 - %s", "[%s] %s%s 实 - %s", "[%s] %s%s 实 - %s");
        add("cmd.money.no_transactions", "No transactions recorded.", "No transactions recorded.", "No transactions recorded.", "No transactions recorded.", "No transactions recorded.", "No transactions recorded.", "No transactions recorded.", "No transactions recorded.", "No transactions recorded.");
        add("cmd.money.export_failed", "Export failed.", "Export failed.", "Export failed.", "Export failed.", "Export failed.", "Export failed.", "Export failed.", "Export failed.", "Export failed.");
        add("cmd.money.exported_to", "Exported to %s", "Exported to %s", "Exported to %s", "Exported to %s", "Exported to %s", "Exported to %s", "Exported to %s", "Exported to %s", "Exported to %s");
        add("cmd.money.synced", "Money state synced.", "Money state synced.", "Money state synced.", "Money state synced.", "Money state synced.", "Money state synced.", "Money state synced.", "Money state synced.", "Money state synced.");
        add("cmd.money.reset", "Money counters reset. Run /balance to initialize total.", "Money counters reset. Run /balance to initialize total.", "Money counters reset. Run /balance to initialize total.", "Money counters reset. Run /balance to initialize total.", "Money counters reset. Run /balance to initialize total.", "Money counters reset. Run /balance to initialize total.", "Money counters reset. Run /balance to initialize total.", "Money counters reset. Run /balance to initialize total.", "Money counters reset. Run /balance to initialize total.");

        add("cmd.talk.disabled", "Talk mode disabled.", "Talk mode disabled.", "Talk mode disabled.", "Talk mode disabled.", "Talk mode disabled.", "Talk mode disabled.", "Talk mode disabled.", "Talk mode disabled.", "Talk mode disabled.");
        add("cmd.talk.enabled_for", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.", "Talk mode enabled for %s. Chat messages will be sent with /msg. Use /talkoff to disable.");
        add("cmd.talk.already_disabled", "Talk mode is already disabled.", "Talk mode is already disabled.", "Talk mode is already disabled.", "Talk mode is already disabled.", "Talk mode is already disabled.", "Talk mode is already disabled.", "Talk mode is already disabled.", "Talk mode is already disabled.", "Talk mode is already disabled.");
        add("cmd.talk.status_off", "Talk mode is OFF. Use /talkto <name> to enable it.", "Talk mode is OFF. Use /talkto <name> to enable it.", "Talk mode is OFF. Use /talkto <name> to enable it.", "Talk mode is OFF. Use /talkto <name> to enable it.", "Talk mode is OFF. Use /talkto <name> to enable it.", "Talk mode is OFF. Use /talkto <name> to enable it.", "Talk mode is OFF. Use /talkto <name> to enable it.", "Talk mode is OFF. Use /talkto <name> to enable it.", "Talk mode is OFF. Use /talkto <name> to enable it.");
        add("cmd.talk.status_on", "Talk mode is ON for %s. Use /talkoff to disable.", "Talk mode is ON for %s. Use /talkoff to disable.", "Talk mode is ON for %s. Use /talkoff to disable.", "Talk mode is ON for %s. Use /talkoff to disable.", "Talk mode is ON for %s. Use /talkoff to disable.", "Talk mode is ON for %s. Use /talkoff to disable.", "Talk mode is ON for %s. Use /talkoff to disable.", "Talk mode is ON for %s. Use /talkoff to disable.", "Talk mode is ON for %s. Use /talkoff to disable.");

        add("auction.per_item", "/item", "/objet", "/objeto", "/Stück", "/oggetto", "/item", "/szt.", "/item", "/adet");
        add("setting.auction_highlight", "Auction House Highlight", "Surlignage Hôtel des ventes", "Resaltado de Casa de Subastas", "Auktionshaus-Highlight", "Evidenziazione Casa d'Aste", "Destaque da Casa de Leilões", "Podświetlanie domu aukcyjnego", "Sorotan rumah lelang", "Müzayede evi vurgusu");
        add("setting.haki_cooldown", "Haki Cooldown", "Recharge Haki", "Enfriamiento de Haki", "Haki-Abklingzeit", "Cooldown Haki", "Recarga de Haki", "Czas odnowienia Haki", "Cooldown Haki", "Haki bekleme süresi");
        add("setting.rarity_icons", "Item Rarity Icons", "Icônes de rareté", "Iconos de rareza", "Seltenheits-Icons", "Icone rarità", "Ícones de raridade", "Ikony rzadkości", "Ikon kelangkaan", "Nadirlik simgeleri");
        add("setting.pet_icons", "Pet Stat Icons", "Icônes stats de familier", "Iconos de stats de mascota", "Haustier-Status-Icons", "Icone statistiche pet", "Ícones de stats do pet", "Ikony statystyk peta", "Ikon stat pet", "Pet istatistik simgeleri");
        add("setting.scrolls_hud", "Scrolls Tracker HUD", "HUD suivi des parchemins", "HUD de pergaminos", "HUD Schriftrollen-Tracker", "HUD tracker pergamene", "HUD rastreador de pergaminhos", "HUD śledzenia zwojów", "HUD pelacak scroll", "Parşömen takip HUD");
        add("setting.xp_hud", "XP HUD (Fruit/Weapon)", "HUD XP (Fruit/Weapon)", "HUD XP (Fruit/Weapon)", "XP-HUD (Fruit/Weapon)", "HUD XP (Fruit/Weapon)", "HUD XP (Fruit/Weapon)", "HUD XP (Fruit/Weapon)", "HUD XP (Fruit/Weapon)", "XP HUD (Fruit/Weapon)");

        add("other.desc1", "General toggles for extra overlays and helpers.", "Options générales pour les overlays et aides.", "Opciones generales para overlays y ayudas.", "Allgemeine Schalter für Overlays und Helfer.", "Opzioni generali per overlay e aiuti.", "Opções gerais para overlays e auxiliares.", "Ogólne przełączniki nakładek i pomocy.", "Toggle umum untuk overlay dan bantuan.", "Ek kaplamalar ve yardımcılar için genel ayarlar.");
        add("other.desc2", "Scroll rows are colored by rarity in the Scrolls HUD panel.", "Les lignes de parchemins sont colorées selon la rareté.", "Las filas de pergaminos tienen color según rareza.", "Schriftrollenzeilen werden nach Seltenheit gefärbt.", "Le righe pergamene sono colorate per rarità.", "Linhas de pergaminhos são coloridas por raridade.", "Wiersze zwojów mają kolor wg rzadkości.", "Baris scroll diwarnai berdasarkan kelangkaan.", "Parşömen satırları nadirliğe göre renklidir.");
        add("other.desc3", "XP panel format: ItemName Level Current/Needed | asc ready | maxed.", "Format panneau XP : Nom Niveau Actuel/Requis | asc prêt | max.", "Formato panel XP: Nombre Nivel Actual/Necesario | asc listo | máximo.", "XP-Panel-Format: Name Level Aktuell/Benötigt | asc bereit | max.", "Formato pannello XP: Nome Livello Attuale/Richiesto | asc pronto | max.", "Formato do painel XP: Nome Nível Atual/Necessário | asc pronto | máx.", "Format panelu XP: Nazwa Poziom Aktualne/Wymagane | asc gotowe | max.", "Format panel XP: Nama Level SaatIni/Dibutuhkan | asc siap | max.", "XP panel biçimi: Ad Seviye Mevcut/Gerekli | asc hazır | max.");

        add("hud.panel.jobs", "Jobs", "Métiers", "Trabajos", "Berufe", "Mestieri", "Profissões", "Prace", "Pekerjaan", "Meslekler");
        add("hud.panel.money", "Money", "Argent", "Dinero", "Geld", "Denaro", "Dinheiro", "Pieniądze", "Uang", "Para");
        add("hud.panel.stats", "Stats", "Stats", "Stats", "Stats", "Stats", "Stats", "Statystyki", "Stat", "İstatistik");
        add("hud.panel.bosses", "Bosses", "Boss", "Jefes", "Bosse", "Boss", "Chefes", "Bossowie", "Boss", "Bosslar");
        add("hud.panel.minibosses", "Minibosses", "Miniboss", "Minibosses", "Minibosse", "Miniboss", "Minibosses", "Minibossy", "Miniboss", "Minibosslar");
        add("hud.panel.event", "Event Timer", "Timer événement", "Temporizador evento", "Event-Timer", "Timer evento", "Temporizador evento", "Timer wydarzeń", "Timer event", "Etkinlik sayacı");
        add("hud.panel.haki", "Haki", "Haki", "Haki", "Haki", "Haki", "Haki", "Haki", "Haki", "Haki");
        add("hud.panel.scrolls", "Scrolls", "Parchemins", "Pergaminos", "Schriftrollen", "Pergamene", "Pergaminhos", "Zwoje", "Scroll", "Parşömenler");
        add("hud.panel.xp", "XP", "XP", "XP", "XP", "XP", "XP", "XP", "XP", "XP");

        add("common.hidden", "Hidden", "Masqué", "Oculto", "Versteckt", "Nascosto", "Oculto", "Ukryte", "Tersembunyi", "Gizli");
        add("common.no_data", "No data", "Aucune donnée", "Sin datos", "Keine Daten", "Nessun dato", "Sem dados", "Brak danych", "Tidak ada data", "Veri yok");
        add("common.selected", "Selected", "Sélectionné", "Seleccionado", "Ausgewählt", "Selezionato", "Selecionado", "Wybrane", "Dipilih", "Seçili");
        add("common.ready", "READY", "PRÊT", "LISTO", "BEREIT", "PRONTO", "PRONTO", "GOTOWE", "SIAP", "HAZIR");
        add("common.unknown", "UNKNOWN", "INCONNU", "DESCONOCIDO", "UNBEKANNT", "SCONOSCIUTO", "DESCONHECIDO", "NIEZNANE", "TIDAK DIKETAHUI", "BİLİNMİYOR");
        add("bosses.wp", "WP", "WP", "WP", "WP", "WP", "WP", "WP", "WP", "WP");
        add("bosses.boss", "Boss", "Boss", "Jefe", "Boss", "Boss", "Chefe", "Boss", "Boss", "Boss");
        add("cooldown.label", "Cooldown: %.1fs", "Recharge : %.1fs", "Enfriamiento: %.1fs", "Abklingzeit: %.1fs", "Cooldown: %.1fs", "Recarga: %.1fs", "Odnowienie: %.1fs", "Cooldown: %.1fs", "Bekleme süresi: %.1fs");
        add("cooldown.haki_ready", "Haki ready", "Haki prêt", "Haki listo", "Haki bereit", "Haki pronto", "Haki pronto", "Haki gotowe", "Haki siap", "Haki hazır");
        add("xp.asc_ready", "asc ready", "asc prêt", "asc listo", "asc bereit", "asc pronto", "asc pronto", "asc gotowe", "asc siap", "asc hazır");
        add("xp.maxed", "maxed", "max", "máximo", "max", "max", "máx", "max", "maks", "maks");
        add("stats.hint_profile_sync", "Run /profile if stats stop syncing", "Lancez /profile si les stats cessent de se synchroniser", "Ejecuta /profile si las stats dejan de sincronizarse", "Führe /profile aus, wenn Stats nicht mehr synchronisieren", "Esegui /profile se le stats smettono di sincronizzarsi", "Use /profile se as stats pararem de sincronizar", "Uruchom /profile jeśli statystyki przestaną się synchronizować", "Jalankan /profile jika stat berhenti sinkron", "İstatistik senkronu durursa /profile çalıştır");

        add("hud.editor.controls", "HUD Edit (.): drag with mouse, wheel resize, 1-9 select, . closes", "Édition HUD (.): glisser souris, molette redimensionne, 1-9 sélection, . ferme", "Edición HUD (.): arrastra con ratón, rueda cambia tamaño, 1-9 selecciona, . cierra", "HUD-Bearbeitung (.): mit Maus ziehen, Rad skaliert, 1-9 wählen, . schließt", "Editor HUD (.): trascina col mouse, rotella ridimensiona, 1-9 seleziona, . chiude", "Edição HUD (.): arraste com mouse, roda redimensiona, 1-9 seleciona, . fecha", "Edycja HUD (.): przeciągaj myszą, kółko zmienia rozmiar, 1-9 wybór, . zamyka", "Edit HUD (.): geser dengan mouse, roda ubah ukuran, 1-9 pilih, . tutup", "HUD Düzenleme (.): fareyle sürükle, tekerlek ölçekler, 1-9 seç, . kapatır");

        add("hud.layout.title", "Minepiece HUD Layout", "Disposition HUD Minepiece", "Diseño HUD de Minepiece", "Minepiece HUD-Layout", "Layout HUD Minepiece", "Layout HUD Minepiece", "Układ HUD Minepiece", "Tata Letak HUD Minepiece", "Minepiece HUD Düzeni");
        add("hud.layout.line1", "HUD Editor: drag panels with mouse, wheel resize.", "Éditeur HUD : glissez les panneaux, molette redimensionne.", "Editor HUD: arrastra paneles con el ratón, rueda redimensiona.", "HUD-Editor: Panels mit Maus ziehen, Mausrad skaliert.", "Editor HUD: trascina i pannelli col mouse, rotella ridimensiona.", "Editor HUD: arraste painéis com o mouse, roda redimensiona.", "Edytor HUD: przeciągaj panele myszą, kółko zmienia rozmiar.", "Editor HUD: geser panel dengan mouse, roda ubah ukuran.", "HUD Düzenleyici: panelleri fareyle sürükle, tekerlek ölçekler.");
        add("hud.layout.panels", "Panels", "Panneaux", "Paneles", "Panels", "Pannelli", "Painéis", "Panele", "Panel", "Paneller");
        add("hud.layout.line3", "Panel colors are configured in the main Minepiece menu.", "Les couleurs des panneaux se règlent dans le menu principal.", "Los colores de panel se configuran en el menú principal.", "Panelfarben werden im Hauptmenü eingestellt.", "I colori pannello si configurano nel menu principale.", "As cores dos painéis são configuradas no menu principal.", "Kolory paneli ustawisz w menu głównym.", "Warna panel diatur di menu utama.", "Panel renkleri ana menüden ayarlanır.");
        add("hud.layout.shortcut", "%s: save and return to game • ESC: back", "%s : enregistrer et jouer • ESC : retour", "%s: guardar y volver al juego • ESC: atrás", "%s: speichern und spielen • ESC: zurück", "%s: salva e torna al gioco • ESC: indietro", "%s: salvar e voltar ao jogo • ESC: voltar", "%s: zapisz i wróć do gry • ESC: wstecz", "%s: simpan dan kembali bermain • ESC: kembali", "%s: kaydet ve oyuna dön • ESC: geri");
        add("hud.layout.line4", ". or ESC to close", ". ou ESC pour fermer", ". o ESC para cerrar", ". oder ESC zum Schließen", ". o ESC per chiudere", ". ou ESC para fechar", ". lub ESC aby zamknąć", ". atau ESC untuk menutup", ". veya ESC ile kapat");
    }

    private UiLocalization() {
    }

    private static void add(
        String key,
        String en,
        String fr,
        String es,
        String de,
        String it,
        String pt,
        String pl,
        String id,
        String tr
    ) {
        STRINGS.put(key, new String[] {en, fr, es, de, it, pt, pl, id, tr});
    }

    public static List<LanguageOption> supportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    public static String normalizeLanguageCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            normalized = normalized.substring(0, dash);
        }
        int underscore = normalized.indexOf('_');
        if (underscore > 0) {
            normalized = normalized.substring(0, underscore);
        }
        return isSupportedLanguage(normalized) ? normalized : DEFAULT_LANGUAGE;
    }

    public static boolean isSupportedLanguage(String code) {
        String normalized = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        for (LanguageOption option : SUPPORTED_LANGUAGES) {
            if (option.code().equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static int languageIndex(String code) {
        String normalized = normalizeLanguageCode(code);
        for (int i = 0; i < SUPPORTED_LANGUAGES.size(); i++) {
            if (SUPPORTED_LANGUAGES.get(i).code().equals(normalized)) {
                return i;
            }
        }
        return 0;
    }

    public static String text(String languageCode, String key) {
        String[] values = STRINGS.get(key);
        if (values == null || values.length == 0) {
            return key;
        }
        int index = languageIndex(languageCode);
        if (index >= 0 && index < values.length && values[index] != null && !values[index].isBlank()) {
            return values[index];
        }
        return values[0] == null ? key : values[0];
    }

    public record LanguageOption(String code, String label) {
    }
}
