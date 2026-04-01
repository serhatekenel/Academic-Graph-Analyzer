package prolab3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GrafEkrani extends JFrame {

    private GrafYonetici grafYonetici;
    private JPanel cizimPaneli;

    // Kontrol Elemanları
    private JTextField txtId;
    private JTextField txtKDegeri;

    // Çizim Nesneleri
    private List<GorselDugum> dugumler;
    private List<GorselKenar> kenarlar;

    private GorselDugum fareUzerindekiDugum = null;
    private final int DUGUM_YARICAP = 25;

    public GrafEkrani(GrafYonetici grafYonetici) {
        this.grafYonetici = grafYonetici;
        this.dugumler = new ArrayList<>();
        this.kenarlar = new ArrayList<>();

        setTitle("Makale Graf Analizcisi - Final [TAM SÜRÜM]");
        setSize(1350, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- 1. ÜST PANEL ---
        JPanel kontrolPaneli = new JPanel();
        kontrolPaneli.setBackground(new Color(230, 240, 255));
        kontrolPaneli.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        // A) H-Core Paneli
        JPanel pnlHCore = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlHCore.setBorder(BorderFactory.createTitledBorder("Analiz ve Genişletme"));
        pnlHCore.setBackground(Color.WHITE);

        String varsayilanId = "";
        if (!grafYonetici.getMakaleler().isEmpty()) {
            varsayilanId = grafYonetici.getMakaleler().keySet().iterator().next();
        }
        pnlHCore.add(new JLabel("Makale ID:"));
        txtId = new JTextField(varsayilanId, 12);
        JButton btnAnaliz = new JButton("H-Core Analiz");
        pnlHCore.add(txtId);
        pnlHCore.add(btnAnaliz);

        // B) İleri Analiz Paneli
        JPanel pnlIleri = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlIleri.setBorder(BorderFactory.createTitledBorder("İleri Analiz"));
        pnlIleri.setBackground(Color.WHITE);
        pnlIleri.add(new JLabel("k:"));
        txtKDegeri = new JTextField("2", 3);
        JButton btnKCore = new JButton("K-Core");
        JButton btnMerkezilik = new JButton("Merkezilik");
        pnlIleri.add(txtKDegeri);
        pnlIleri.add(btnKCore);
        pnlIleri.add(btnMerkezilik);

        // C) İstatistik Butonu
        JButton btnIstatistik = new JButton("Genel İstatistikler");
        btnIstatistik.setBackground(new Color(255, 200, 200));

        kontrolPaneli.add(pnlHCore);
        kontrolPaneli.add(pnlIleri);
        kontrolPaneli.add(btnIstatistik);
        add(kontrolPaneli, BorderLayout.NORTH);

        // --- 2. ORTA PANEL ---
        cizimPaneli = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                cizimYap(g);
            }
        };
        cizimPaneli.setBackground(Color.WHITE);
        add(cizimPaneli, BorderLayout.CENTER);

        // --- OLAYLAR ---
        cizimPaneli.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                GorselDugum tiklanan = dugumBul(e.getX(), e.getY());
                if (tiklanan != null) {
                    grafiGenislet(tiklanan);
                }
            }
        });

        cizimPaneli.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                fareUzerindekiDugum = dugumBul(e.getX(), e.getY());
                cizimPaneli.repaint();
            }
        });

        btnAnaliz.addActionListener(e -> hCoreBaslat());
        btnKCore.addActionListener(e -> kCoreBaslat());
        btnMerkezilik.addActionListener(e -> merkezilikGoster());

        // İstatistik Butonu Olayı
        btnIstatistik.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, grafYonetici.getGenelIstatistikler(), "Genel Graf İstatistikleri", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // --- H-CORE MANTIĞI ---
    private void hCoreBaslat() {
        String id = txtId.getText().trim();
        if (!grafYonetici.getMakaleler().containsKey(id)) {
            return;
        }

        dugumler.clear();
        kenarlar.clear();

        Makale merkez = grafYonetici.getMakaleler().get(id);
        GorselDugum merkezDugum = new GorselDugum(merkez, cizimPaneli.getWidth() / 2, cizimPaneli.getHeight() / 2, Color.RED);
        dugumler.add(merkezDugum);
        grafiGenislet(merkezDugum);
    }

    private void grafiGenislet(GorselDugum merkezDugum) {
        List<String> hCoreIds = grafYonetici.getHCore(merkezDugum.makale.getId());

        // 1. Önce ekrandaki MEVCUT tüm düğümleri "Eski" rengine (Gri) çevir
        for (GorselDugum gd : dugumler) {
            gd.renk = Color.LIGHT_GRAY;
        }

        // 2. Tıklanan düğümü tekrar belirgin yap (Kırmızı)
        merkezDugum.renk = Color.RED;
        // ------------------------------------------

        if (hCoreIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bu makalenin H-Index'i 0 veya H-Core listesi boş.");
            cizimPaneli.repaint();
            return;
        }

        double aci = 360.0 / hCoreIds.size();
        int r = 200;

        for (int i = 0; i < hCoreIds.size(); i++) {
            String yeniId = hCoreIds.get(i);
            GorselDugum varOlan = null;

            for (GorselDugum gd : dugumler) {
                if (gd.makale.getId().equals(yeniId)) {
                    varOlan = gd;
                    break;
                }
            }

            GorselDugum hedefDugum;
            if (varOlan != null) {
                hedefDugum = varOlan;
                // Eğer düğüm zaten varsa ve 'Gri' olduysa, bunun bu seferki genişletmenin
                // bir parçası olduğunu göstermek için rengini tekrar Turuncu yap
                hedefDugum.renk = Color.ORANGE;
            } else {
                int x = (int) (merkezDugum.x + r * Math.cos(Math.toRadians(i * aci)));
                int y = (int) (merkezDugum.y + r * Math.sin(Math.toRadians(i * aci)));

                x = Math.max(50, Math.min(cizimPaneli.getWidth() - 50, x));
                y = Math.max(50, Math.min(cizimPaneli.getHeight() - 50, y));

                // 3. YENİ EKLENEN düğümleri Turuncu yap
                hedefDugum = new GorselDugum(grafYonetici.getMakaleler().get(yeniId), x, y, Color.ORANGE);
                dugumler.add(hedefDugum);
            }

            tumKenarlariGuncelle(hedefDugum);
        }

        // Tıkladığında H-Index ve Median bilgisini gösteren mesaj
        String bilgiMesaji = String.format(
                "Genişletilen Makale: %s\nH-Index: %d\nH-Median: %.1f\n\nYeni düğümler Turuncu, öncekiler Gri renk ile gösterildi.",
                merkezDugum.makale.getTitle(),
                grafYonetici.calculateHIndex(merkezDugum.makale.getId()),
                grafYonetici.calculateHMedian(merkezDugum.makale.getId())
        );
        JOptionPane.showMessageDialog(this, bilgiMesaji, "Analiz Sonucu", JOptionPane.INFORMATION_MESSAGE);

        cizimPaneli.repaint();
    }

    // Yeni eklenen veya güncellenen bir düğümün, ekrandaki diğer HERKESLE bağlantısını kontrol et
    private void tumKenarlariGuncelle(GorselDugum yeniDugum) {
        for (GorselDugum diger : dugumler) {
            if (diger == yeniDugum) {
                continue;
            }

            // Eğer yeni düğüm, diğerine atıf vermişse VEYA diğer, yeniye atıf vermişse çiz
            // Yönlü çizim yapıyoruz: Outgoing (Referanslar)
            if (yeniDugum.makale.getReferences().contains(diger.makale.getId())) {
                kenarEkle(yeniDugum, diger); // Yeniden Eskiye Ok
            }
            if (diger.makale.getReferences().contains(yeniDugum.makale.getId())) {
                kenarEkle(diger, yeniDugum); // Eskiden Yeniye Ok
            }
        }
    }

    // --- K-CORE MANTIĞI ---
    private void kCoreBaslat() {
        try {
            int k = Integer.parseInt(txtKDegeri.getText().trim());
            List<String> ids = grafYonetici.getKCoreList(k);
            if (ids.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Sonuç yok.");
                return;
            }

            dugumler.clear();
            kenarlar.clear();
            Random rnd = new Random();
            for (String id : ids) {
                dugumler.add(new GorselDugum(grafYonetici.getMakaleler().get(id),
                        50 + rnd.nextInt(cizimPaneli.getWidth() - 100),
                        50 + rnd.nextInt(cizimPaneli.getHeight() - 100), new Color(100, 149, 237)));
            }

            // K-Core'da tüm düğümler arası ilişkilere bak
            for (GorselDugum d1 : dugumler) {
                tumKenarlariGuncelle(d1);
            }
            cizimPaneli.repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    private void merkezilikGoster() {
        cizimPaneli.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Map<String, Double> skorlar = grafYonetici.calculateBetweennessCentrality();
        cizimPaneli.setCursor(Cursor.getDefaultCursor());

        List<Map.Entry<String, Double>> liste = new ArrayList<>(skorlar.entrySet());
        liste.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder("En Merkezi 15 Makale:\n");
        for (int i = 0; i < 15 && i < liste.size(); i++) {
            Makale m = grafYonetici.getMakaleler().get(liste.get(i).getKey());
            sb.append(String.format("%.2f - %s\n", liste.get(i).getValue(), m.getTitle()));
        }
        JTextArea ta = new JTextArea(sb.toString());
        JOptionPane.showMessageDialog(this, new JScrollPane(ta));
    }

    // --- YARDIMCILAR ---
    private void kenarEkle(GorselDugum d1, GorselDugum d2) {
        for (GorselKenar k : kenarlar) {
            if (k.d1 == d1 && k.d2 == d2) {
                return; // Zaten varsa ekleme
            }
        }
        kenarlar.add(new GorselKenar(d1, d2));
    }

    private GorselDugum dugumBul(int x, int y) {
        for (GorselDugum gd : dugumler) {
            if (Math.sqrt(Math.pow(x - gd.x, 2) + Math.pow(y - gd.y, 2)) <= DUGUM_YARICAP) {
                return gd;
            }
        }
        return null;
    }

    private void cizimYap(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Kenar yumuşatma (Anti-aliasing) açıyoruz ki çizgiler tırtıklı olmasın
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // -------------------------------------------------------------
        // 1. ADIM: YEŞİL KENARLAR (ID Sırasına Göre Bağlantı) 
        // -------------------------------------------------------------
        // Bu kenarlar diğerlerinin altında kalsın diye en önce çiziyoruz.
        g2.setColor(new Color(34, 139, 34)); // Orman Yeşili (Forest Green)
        g2.setStroke(new BasicStroke(2.0f)); // Yeşil çizgiler biraz kalın 

        for (GorselDugum d1 : dugumler) {
            // GrafYöneticisi'nden bu ID'den sonra gelen ID'yi soruyoruz
            String sonrakiId = grafYonetici.getSonrakiId(d1.makale.getId());

            if (sonrakiId != null) {
                // Eğer o "sonraki ID" şu an ekranda çizili düğümler arasındaysa çiz
                for (GorselDugum d2 : dugumler) {
                    if (d2.makale.getId().equals(sonrakiId)) {
                        g2.drawLine(d1.x, d1.y, d2.x, d2.y);
                        break;
                    }
                }
            }
        }
        g2.setStroke(new BasicStroke(1.0f)); // Kalınlığı normale döndür (Siyahlar için)

        // -------------------------------------------------------------
        // 2. ADIM: SİYAH KENARLAR (Referans İlişkisi) [cite: 16]
        // -------------------------------------------------------------
        for (GorselKenar k : kenarlar) {
            g2.setColor(Color.LIGHT_GRAY); // Çizgiler gri
            g2.drawLine(k.d1.x, k.d1.y, k.d2.x, k.d2.y);

            // Ok ucu çizimi (Siyah oklar yönü belli etmeli)
            drawArrowHead(g2, k.d1.x, k.d1.y, k.d2.x, k.d2.y);
        }

        // -------------------------------------------------------------
        // 3. ADIM: DÜĞÜMLER (Makaleler)
        // -------------------------------------------------------------
        for (GorselDugum gd : dugumler) {
            // Dairenin içini boya
            g2.setColor(gd.renk);
            g2.fillOval(gd.x - DUGUM_YARICAP, gd.y - DUGUM_YARICAP, DUGUM_YARICAP * 2, DUGUM_YARICAP * 2);

            // Dairenin çerçevesini çiz
            g2.setColor(Color.BLACK);
            g2.drawOval(gd.x - DUGUM_YARICAP, gd.y - DUGUM_YARICAP, DUGUM_YARICAP * 2, DUGUM_YARICAP * 2);

            // Atıf sayısını içine yaz (Kalabalık değilse)
            if (dugumler.size() < 100) {
                String yazi = String.valueOf(gd.makale.getCitationCount());
                // Yazıyı ortalamak için genişliğini hesapla
                int w = g2.getFontMetrics().stringWidth(yazi);
                // Dikey ortalama için +5 kaydırma (Göz kararı)
                g2.drawString(yazi, gd.x - w / 2, gd.y + 5);
            }
        }

        // -------------------------------------------------------------
        // 4. ADIM: BİLGİ KARTI (Hover) [cite: 68]
        // -------------------------------------------------------------
        // En son çiziyoruz ki her şeyin üstünde görünsün
        if (fareUzerindekiDugum != null) {
            bilgiKartiCiz(g2, fareUzerindekiDugum);
        }
    }

    private void drawArrowHead(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 10;
        int dX = (int) (Math.cos(angle) * DUGUM_YARICAP); // Dairenin kenarına gelsin diye
        int dY = (int) (Math.sin(angle) * DUGUM_YARICAP);

        int targetX = x2 - dX;
        int targetY = y2 - dY;

        g2.setColor(Color.BLACK);
        g2.fillPolygon(new int[]{targetX, targetX - arrowSize, targetX - arrowSize},
                new int[]{targetY, targetY - arrowSize, targetY + arrowSize}, 3); // Basit üçgen
    }

    private void bilgiKartiCiz(Graphics2D g2, GorselDugum dugum) {
        Makale m = dugum.makale;
        int hIndex = grafYonetici.calculateHIndex(m.getId());
        double hMedian = grafYonetici.calculateHMedian(m.getId()); // HESAPLAMA

        String[] satirlar = {
            "ID: " + m.getId(),
            "Başlık: " + (m.getTitle().length() > 35 ? m.getTitle().substring(0, 35) + "..." : m.getTitle()),
            "Yazar: " + (m.getAuthors().isEmpty() ? "?" : m.getAuthors().get(0)),
            "Yıl: " + m.getYear(),
            "Atıf: " + m.getCitationCount(),
            "H-Index: " + hIndex,
            "H-Median: " + hMedian // GÖSTERİM 
        };

        int w = 300, h = 160, x = dugum.x + 30, y = dugum.y - 30;
        if (x + w > cizimPaneli.getWidth()) {
            x = dugum.x - w - 30;
        }
        if (y + h > cizimPaneli.getHeight()) {
            y = dugum.y - h;
        }

        g2.setColor(new Color(255, 255, 225));
        g2.fillRect(x, y, w, h);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y, w, h);
        for (int i = 0; i < satirlar.length; i++) {
            g2.drawString(satirlar[i], x + 10, y + 20 + (i * 20));
        }
    }

    private class GorselDugum {

        Makale makale;
        int x, y;
        Color renk;

        public GorselDugum(Makale m, int x, int y, Color c) {
            this.makale = m;
            this.x = x;
            this.y = y;
            this.renk = c;
        }
    }

    private class GorselKenar {

        GorselDugum d1, d2;

        public GorselKenar(GorselDugum a, GorselDugum b) {
            d1 = a;
            d2 = b;
        }
    }
}
