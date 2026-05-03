package pk.edu.nu.isb.bms.models;

public class Faculty {
    private Long id;
    private String name;
    private String department;
    private String imageUrl;
    private String bio;

    public Faculty() {}

    // Backward compatible constructor (no bio)
    public Faculty(Long id, String name, String department, String imageUrl) {
        this(id, name, department, imageUrl, null);
    }

    public Faculty(Long id, String name, String department, String imageUrl, String bio) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.imageUrl = imageUrl;
        this.bio = bio;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
