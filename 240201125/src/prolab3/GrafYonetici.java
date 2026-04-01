package prolab3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class GrafYonetici {

    // Tüm makaleleri ID'lerine göre hızlıca bulmak için Map kullanıyoruz
    private Map<String, Makale> makaleler;

    public GrafYonetici() {
        this.makaleler = new HashMap<>();
    }

    // JSON Dosyasını okuyup nesneleri oluşturur
    public void jsonDosyasiOku(String dosyaYolu) {
        try (BufferedReader br = new BufferedReader(new FileReader(dosyaYolu))) {
            String line;
            Makale currentMakale = null;

            // Okuma modları (State Machine mantığı)
            // Bu yöntem satır atlamayı engeller
            boolean authorOkumaModu = false;
            boolean refOkumaModu = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                // 1. Yazar Okuma Modundaysak
                if (authorOkumaModu) {
                    if (line.startsWith("]")) {
                        authorOkumaModu = false;
                    } else {
                        String author = extractValue(line);
                        if (currentMakale != null && !author.isEmpty()) {
                            currentMakale.addAuthor(author);
                        }
                    }
                    continue; // Satırı işledik, döngü başına dön
                }

                // 2. Referans Okuma Modundaysak
                if (refOkumaModu) {
                    if (line.startsWith("]")) {
                        refOkumaModu = false;
                    } else {
                        String refId = extractValue(line);
                        if (currentMakale != null && !refId.isEmpty()) {
                            currentMakale.addReference(refId);
                        }
                    }
                    continue; // Satırı işledik, döngü başına dön
                }

                // 3. Normal Mod (Anahtarları arıyoruz)
                if (line.equals("{")) {
                    currentMakale = new Makale();
                } else if (line.startsWith("\"id\":")) {
                    if (currentMakale != null) {
                        currentMakale.setId(extractValue(line));
                    }
                } else if (line.startsWith("\"title\":")) {
                    if (currentMakale != null) {
                        currentMakale.setTitle(extractValue(line));
                    }
                } else if (line.startsWith("\"year\":")) {
                    if (currentMakale != null) {
                        try {
                            String yearStr = line.replace("\"year\":", "").replace(",", "").trim();
                            currentMakale.setYear(Integer.parseInt(yearStr));
                        } catch (NumberFormatException e) {
                            /* Yıl formatı hatalıysa geç */ }
                    }
                } else if (line.startsWith("\"authors\":")) {
                    if (!line.contains("]")) {
                        authorOkumaModu = true; // Modu aç (eğer tek satırda bitmiyorsa)
                    }
                } else if (line.startsWith("\"referenced_works\":")) {
                    if (!line.contains("]")) {
                        refOkumaModu = true; // Modu aç (eğer tek satırda bitmiyorsa)
                    }
                } else if (line.startsWith("}") || line.startsWith("},")) {
                    // Nesne bitti, listeye ekle
                    if (currentMakale != null && currentMakale.getId() != null) {
                        makaleler.put(currentMakale.getId(), currentMakale);
                        currentMakale = null; // Sıfırla
                    }
                }
            }

            System.out.println("Dosya okuma tamamlandı. Toplam Makale: " + makaleler.size());
            referanslariHesapla(); // Atıf sayılarını hesapla
            idleriSirala();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // String içindeki gereksiz karakterleri temizler
    private String extractValue(String line) {
        return line.replace("\"", "")
                .replace(",", "")
                .replace("id:", "")
                .replace("title:", "")
                .trim();
    }

    // Alınan referans sayılarını (In-degree) hesaplar
    private void referanslariHesapla() {
        for (Makale m : makaleler.values()) {
            for (String refId : m.getReferences()) {
                Makale refMakale = makaleler.get(refId);
                if (refMakale != null) {
                    refMakale.incrementCitationCount(); // Sayıyı artır
                    refMakale.addCitedBy(m.getId());    // Listeye ekle
                }
            }
        }
    }

    public Map<String, Makale> getMakaleler() {
        return makaleler;
    }

    public int calculateHIndex(String makaleId) {
        Makale m = makaleler.get(makaleId);
        if (m == null || m.getCitedBy().isEmpty()) {
            return 0;
        }

        // Bize atıf yapan makalelerin "atıf sayılarını" bir listeye al
        List<Integer> citationsOfCiters = new ArrayList<>();
        for (String cilerId : m.getCitedBy()) {
            Makale ciler = makaleler.get(cilerId);
            if (ciler != null) {
                citationsOfCiters.add(ciler.getCitationCount());
            }
        }

        // Büyükten küçüğe sırala (Örn: 17, 9, 6, 3, 2)
        Collections.sort(citationsOfCiters, Collections.reverseOrder());

        // H-Index bulma
        int hIndex = 0;
        for (int i = 0; i < citationsOfCiters.size(); i++) {
            if (citationsOfCiters.get(i) >= i + 1) {
                hIndex = i + 1;
            } else {
                break;
            }
        }
        return hIndex;
    }

    // H-Core Listesini (Atıf yapan en nitelikli makaleleri) döner
    public List<String> getHCore(String makaleId) {
        int hIndex = calculateHIndex(makaleId);
        List<String> hCoreIds = new ArrayList<>();
        Makale m = makaleler.get(makaleId);
        if (m == null) {
            return hCoreIds;
        }

        // Atıf yapanları (citedBy) bul ve atıf sayılarına göre sırala
        List<Makale> citers = new ArrayList<>();
        for (String id : m.getCitedBy()) {
            if (makaleler.containsKey(id)) {
                citers.add(makaleler.get(id));
            }
        }

        // Lambda ile sıralama (Atıf sayısına göre büyükten küçüğe)
        citers.sort((a, b) -> b.getCitationCount() - a.getCitationCount());

        // İlk hIndex kadarını listeye al (Bunlar H-Core kümesidir)
        for (int i = 0; i < hIndex && i < citers.size(); i++) {
            hCoreIds.add(citers.get(i).getId());
        }
        return hCoreIds;
    }

    // H-Median hesaplar (H-Core içindeki atıfların medyanı)
    public double calculateHMedian(String makaleId) {
        List<String> hCoreIds = getHCore(makaleId);
        if (hCoreIds.isEmpty()) {
            return 0;
        }

        List<Integer> citations = new ArrayList<>();
        for (String id : hCoreIds) {
            citations.add(makaleler.get(id).getCitationCount());
        }

        // Medyan bulmak için sırala
        Collections.sort(citations);

        int n = citations.size();
        if (n % 2 == 1) {
            return citations.get(n / 2); // Tek sayıysa ortadaki
        } else {
            return (citations.get((n - 1) / 2) + citations.get(n / 2)) / 2.0; // Çiftse ortadaki ikisinin ortalaması
        }
    }
    // --- 2.3. GRAF ANALİZ METRİKLERİ ---

    // Yardımcı Metot: Yönsüz komşuları getirir (Giden + Gelen oklar)
    private List<String> getYonsuzKomsular(Makale m) {
        List<String> komsular = new ArrayList<>(m.getReferences()); // Gidenler
        for (String gelenId : m.getCitedBy()) { // Gelenler
            if (!komsular.contains(gelenId)) {
                komsular.add(gelenId);
            }
        }
        return komsular;
    }

    // 1. Betweenness Centrality Hesaplama 
    public Map<String, Double> calculateBetweennessCentrality() {
        Map<String, Double> centralityScores = new HashMap<>();
        for (String id : makaleler.keySet()) {
            centralityScores.put(id, 0.0);
        }

        // Her düğüm için BFS çalıştır
        for (String startId : makaleler.keySet()) {
            // Yığın ve Kuyruk yapıları
            java.util.Stack<String> stack = new java.util.Stack<>();
            java.util.Queue<String> queue = new java.util.LinkedList<>();

            // Yollar ve Mesafeler
            Map<String, List<String>> predecessors = new HashMap<>();
            Map<String, Integer> distance = new HashMap<>();
            Map<String, Integer> sigma = new HashMap<>(); // Kısa yol sayısı

            for (String id : makaleler.keySet()) {
                predecessors.put(id, new ArrayList<>());
                distance.put(id, -1);
                sigma.put(id, 0);
            }

            distance.put(startId, 0);
            sigma.put(startId, 1);
            queue.add(startId);

            while (!queue.isEmpty()) {
                String v = queue.poll();
                stack.push(v);

                Makale vMakale = makaleler.get(v);
                if (vMakale == null) {
                    continue;
                }

                for (String w : getYonsuzKomsular(vMakale)) {
                    if (!makaleler.containsKey(w)) {
                        continue;
                    }

                    // W ilk kez keşfediliyorsa
                    if (distance.get(w) < 0) {
                        queue.add(w);
                        distance.put(w, distance.get(v) + 1);
                    }
                    // En kısa yol ise sayacı artır
                    if (distance.get(w) == distance.get(v) + 1) {
                        sigma.put(w, sigma.get(w) + sigma.get(v));
                        predecessors.get(w).add(v);
                    }
                }
            }

            // Geriye doğru yayılım (Dependency accumulation)
            Map<String, Double> delta = new HashMap<>();
            for (String id : makaleler.keySet()) {
                delta.put(id, 0.0);
            }

            while (!stack.isEmpty()) {
                String w = stack.pop();
                for (String v : predecessors.get(w)) {
                    double c = ((double) sigma.get(v) / sigma.get(w)) * (1.0 + delta.get(w));
                    delta.put(v, delta.get(v) + c);
                }
                if (!w.equals(startId)) {
                    centralityScores.put(w, centralityScores.get(w) + delta.get(w));
                }
            }
        }

        // Yönsüz graf olduğu için skoru 2'ye bölüyoruz
        for (String id : centralityScores.keySet()) {
            centralityScores.put(id, centralityScores.get(id) / 2.0);
        }
        return centralityScores;
    }

    // 2. K-Core Decomposition Hesaplama
    // Verilen k değerine göre grafı budar ve kalan düğümleri döndürür
    public List<String> getKCoreList(int k) {
        // Orijinal grafı bozmamak için geçici bir kopya (derece haritası) oluştur
        Map<String, Integer> degrees = new HashMap<>();
        List<String> activeNodes = new ArrayList<>(makaleler.keySet());

        // Başlangıç derecelerini hesapla (Yönsüz)
        for (String id : activeNodes) {
            degrees.put(id, getYonsuzKomsular(makaleler.get(id)).size());
        }

        boolean degisimOldu = true;
        while (degisimOldu) {
            degisimOldu = false;
            List<String> silinecekler = new ArrayList<>();

            // K derecesinden küçük olanları tespit et
            for (String id : activeNodes) {
                if (degrees.get(id) < k) {
                    silinecekler.add(id);
                }
            }

            // Eğer silinecek düğüm varsa sil ve komşularının derecesini düşür
            if (!silinecekler.isEmpty()) {
                degisimOldu = true;
                activeNodes.removeAll(silinecekler);

                for (String silinenId : silinecekler) {
                    Makale silinen = makaleler.get(silinenId);
                    if (silinen == null) {
                        continue;
                    }

                    for (String komsusu : getYonsuzKomsular(silinen)) {
                        if (degrees.containsKey(komsusu)) {
                            degrees.put(komsusu, degrees.get(komsusu) - 1);
                        }
                    }
                }
            }
        }
        return activeNodes; // Geriye sadece K-Core düğümleri kalır
    }

    // Genel istatistik metnini hazırlar
    // ID sıralı listeyi tutmak için (Yeşil kenarlar için gerekli)
    private List<String> siraliIdListesi;

    // Dosya okuma bittikten sonra
    private void idleriSirala() {
        siraliIdListesi = new ArrayList<>(makaleler.keySet());
        Collections.sort(siraliIdListesi); // String olarak artan sırada sırala
    }

    // Bir makalenin ID sıralamasında bir sonrakini bulur (Yeşil kenar hedefi)
    public String getSonrakiId(String currentId) {
        if (siraliIdListesi == null) {
            idleriSirala();
        }
        int index = Collections.binarySearch(siraliIdListesi, currentId);
        if (index >= 0 && index < siraliIdListesi.size() - 1) {
            return siraliIdListesi.get(index + 1);
        }
        return null;
    }

    // İSTATİSTİK METODU
    public String getGenelIstatistikler() {
        int toplamMakale = makaleler.size();
        int toplamSiyahKenar = 0; // Outgoing (Verilen)
        int toplamAlinan = 0;     // Incoming (Alınan)

        Makale enCokRefVeren = null;
        Makale enCokRefAlan = null;

        for (Makale m : makaleler.values()) {
            int verilen = m.getReferences().size();
            int alinan = m.getCitationCount();

            toplamSiyahKenar += verilen;
            toplamAlinan += alinan;

            if (enCokRefVeren == null || verilen > enCokRefVeren.getReferences().size()) {
                enCokRefVeren = m;
            }
            if (enCokRefAlan == null || alinan > enCokRefAlan.getCitationCount()) {
                enCokRefAlan = m;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Toplam Makale (Düğüm): ").append(toplamMakale).append("\n");
        sb.append("Toplam Siyah Kenar (Referans): ").append(toplamSiyahKenar).append("\n");
        sb.append("--------------------------------\n");
        sb.append("Toplam Verilen Referans: ").append(toplamSiyahKenar).append("\n");
        sb.append("Toplam Alınan Referans: ").append(toplamAlinan).append("\n");
        sb.append("--------------------------------\n");
        if (enCokRefAlan != null) {
            sb.append("En Çok Ref. ALAN: \n   ID: ").append(enCokRefAlan.getId()).append("\n   Sayı: ").append(enCokRefAlan.getCitationCount()).append("\n");
        }
        if (enCokRefVeren != null) {
            sb.append("En Çok Ref. VEREN: \n   ID: ").append(enCokRefVeren.getId()).append("\n   Sayı: ").append(enCokRefVeren.getReferences().size()).append("\n");
        }
        return sb.toString();
    }
}
