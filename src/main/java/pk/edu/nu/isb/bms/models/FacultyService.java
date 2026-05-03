package pk.edu.nu.isb.bms.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FacultyService {

    private final List<Faculty> faculty = new ArrayList<>();
    private final FacultyRepository facultyRepository;
    private final ReviewRepository reviewRepository;

    public FacultyService(FacultyRepository facultyRepository, ReviewRepository reviewRepository) {
        this.facultyRepository = facultyRepository;
        this.reviewRepository = reviewRepository;
    }

    @PostConstruct
    public void loadFaculty() {
        // If DB has faculty records, load from DB
        List<FacultyEntity> entities = facultyRepository.findAll();
        if (!entities.isEmpty()) {
            for (FacultyEntity e : entities) {
                List<Review> reviews = reviewRepository.findByFaculty_Id(e.getId());
                Faculty dto = new Faculty(e.getId(), e.getName(), e.getDepartment() == null ? "Unknown" : e.getDepartment().getName(), e.getImagePath(), e.getBio());
                faculty.add(dto);
            }
            System.out.println("Faculty loaded from DB: " + faculty.size());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();

        // First: try to load optional JSON metadata to get departments for known people
        Map<String, String> deptByName = new HashMap<>();
        try {
            var resource = new ClassPathResource("data/faculty.json");
            InputStream is = null;
            if (resource.exists()) {
                is = resource.getInputStream();
            } else {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream("data/faculty.json");
            }
            if (is != null) {
                List<Faculty> list = mapper.readValue(is, new TypeReference<List<Faculty>>() {});
                if (list != null) {
                    for (Faculty f : list) {
                        if (f.getName() != null && f.getDepartment() != null) {
                            deptByName.put(normalizeName(f.getName()), f.getDepartment());
                        }
                    }
                }
            }
        } catch (IOException e) {
            // ignore; we'll build from images
            System.out.println("Could not read data/faculty.json: " + e.getMessage());
        }

        // Second: robustly collect image filenames from static/images
        Set<String> filenames = new LinkedHashSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        String[] patterns = new String[]{"classpath*:static/images/*", "classpath*:static/images/*.*", "classpath:/static/images/*"};
        for (String pattern : patterns) {
            try {
                Resource[] resources = resolver.getResources(pattern);
                if (resources != null) {
                    for (Resource res : resources) {
                        String fn = res.getFilename();
                        if (fn != null) filenames.add(fn);
                    }
                }
            } catch (IOException ignored) {
                // continue trying other patterns
            }
        }

        // Fallback: try listing via classloader if the images are served from file system
        if (filenames.isEmpty()) {
            try {
                URL dirUrl = Thread.currentThread().getContextClassLoader().getResource("static/images");
                if (dirUrl != null && "file".equals(dirUrl.getProtocol())) {
                    File dir = new File(dirUrl.toURI());
                    if (dir.exists() && dir.isDirectory()) {
                        Files.list(dir.toPath())
                                .map(p -> p.getFileName().toString())
                                .forEach(filenames::add);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        // Remove obvious non-person images and sort
        List<String> sortedFilenames = filenames.stream()
                .filter(fn -> fn != null && !fn.startsWith("."))
                .filter(fn -> !fn.equalsIgnoreCase("nuces.jpg"))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        int id = 1;
        for (String filename : sortedFilenames) {
            String name = filename;
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
            name = name.replace('_', ' ').replace('-', ' ').trim();
            name = name.replaceAll("%20", " ");
            String normalized = normalizeName(name);
            String dept = deptByName.getOrDefault(normalized, "Unknown");
            Faculty f = new Faculty((long) id, name, dept, "/images/" + filename);
            faculty.add(f);
            id++;
        }

        if (faculty.isEmpty()) {
            System.out.println("No images found under static/images; falling back to default list.");
            loadDefaultFaculty();
        }

        System.out.println("Faculty loaded (from images): " + faculty.size());
    }

    private static String normalizeName(String n) {
        return n == null ? "" : n.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private void loadDefaultFaculty() {
        faculty.clear();
        faculty.add(new Faculty(1L, "Dr. Ahmad Raza Shahid", "CS", "/images/Ahmad Raza Shahid.webp"));
        faculty.add(new Faculty(2L, "Dr. Ali Zeeshan Ijaz", "AI & DS", "/images/Dr. Ali Zeeshan Ijaz.webp"));
        faculty.add(new Faculty(3L, "Dr. Hina Ayaz", "SE", "/images/Dr. Hina Ayaz.webp"));
        faculty.add(new Faculty(4L, "Ms. Asma Tufail", "CS", "/images/Ms. Asma Tufail.webp"));
    }

    public List<Faculty> findAll() {
        return new ArrayList<>(faculty);
    }

    public List<Faculty> search(String query, String dept) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return faculty.stream()
                .filter(f -> (q.isEmpty() || (f.getName() != null && f.getName().toLowerCase(Locale.ROOT).contains(q))))
                .filter(f -> (dept == null || dept.equals("All") || (f.getDepartment() != null && f.getDepartment().equalsIgnoreCase(dept))))
                .collect(Collectors.toList());
    }

    public Optional<Faculty> findById(Long id) {
        // First try DB
        try {
            Optional<FacultyEntity> maybe = facultyRepository.findById(id);
            if (maybe.isPresent()) {
                FacultyEntity e = maybe.get();
                Faculty dto = new Faculty(e.getId(), e.getName(), e.getDepartment() == null ? "Unknown" : e.getDepartment().getName(), e.getImagePath(), e.getBio());
                return Optional.of(dto);
            }
        } catch (Exception ignored) {
            // repository may not exist in some contexts; fall back to in-memory
        }

        return faculty.stream().filter(f -> f.getId() != null && f.getId().equals(id)).findFirst();
    }
}
