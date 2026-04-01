package prolab3;

import java.util.ArrayList;
import java.util.List;

public class Makale {

    private String id;
    private String title;
    private List<String> authors;
    private int year;
    private String doi;
    private String venue;
    private List<String> references; // referenced_works (Verdiği referanslar - Çıkan kenarlar)
    private int citationCount;       // Bize referans veren sayısı (Gelen kenarlar - Bunu hesaplayacağız)
    private List<String> citedBy;

    public Makale() {
        this.authors = new ArrayList<>();
        this.references = new ArrayList<>();
        this.citationCount = 0;
        this.citedBy = new ArrayList<>();
    }

    // Getter ve Setter Metotları
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void addAuthor(String author) {
        this.authors.add(author);
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<String> getReferences() {
        return references;
    }

    public void addReference(String refId) {
        this.references.add(refId);
    }

    public int getCitationCount() {
        return citationCount;
    }

    public void incrementCitationCount() {
        this.citationCount++;
    }

    @Override
    public String toString() {
        return "ID: " + id + " | Title: " + title + " | Refs: " + references.size();
    }

    public List<String> getCitedBy() {
        return citedBy;
    }

    public void addCitedBy(String id) {
        this.citedBy.add(id);
    }
}
