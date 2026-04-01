package prolab3;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // 1. Veriyi Yükle
        GrafYonetici graf = new GrafYonetici();
        graf.jsonDosyasiOku("C:\\Users\\srhte\\OneDrive\\Masaüstü\\240201125\\data.json");

        // 2. EN ÇOK ATIF ALAN 5 MAKALEYİ BUL
        if (!graf.getMakaleler().isEmpty()) {
            System.out.println("\n--- TEST İÇİN EN POPÜLER MAKALELER ---");
            
            List<Makale> tumMakaleler = new ArrayList<>(graf.getMakaleler().values());
            // Atıf sayısına göre büyükten küçüğe sırala
            tumMakaleler.sort((m1, m2) -> m2.getCitationCount() - m1.getCitationCount());
            
            for (int i = 0; i < 5 && i < tumMakaleler.size(); i++) {
                Makale m = tumMakaleler.get(i);
                System.out.println("Atıf: " + m.getCitationCount() + " | ID: " + m.getId());
            }
            System.out.println("--------------------------------------\n");
        }

        // 3. Arayüzü Başlat
        SwingUtilities.invokeLater(() -> {
            GrafEkrani ekran = new GrafEkrani(graf);
            ekran.setVisible(true);
        });
    }
}