package pk.edu.nu.isb.bms.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// This annotation enables Mockito for this test class
@ExtendWith(MockitoExtension.class)
class FacultyServiceTest {

    // @Mock creates "fake" versions of your database repositories
    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private ReviewRepository reviewRepository;

    // @InjectMocks creates your Service and injects the fakes into it
    @InjectMocks
    private FacultyService facultyService;

    private FacultyEntity mockFacultyEntity;
    private Department mockDepartment;

    @BeforeEach
    void setUp() {
        // We set up some dummy data before each test runs
        mockDepartment = mock(Department.class);
        when(mockDepartment.getName()).thenReturn("CS");

        mockFacultyEntity = mock(FacultyEntity.class);
        when(mockFacultyEntity.getId()).thenReturn(1L);
        when(mockFacultyEntity.getName()).thenReturn("Dr. Ahmad Raza Shahid");
        when(mockFacultyEntity.getDepartment()).thenReturn(mockDepartment);
        when(mockFacultyEntity.getImagePath()).thenReturn("/images/ahmad.webp");
        when(mockFacultyEntity.getBio()).thenReturn("Great professor.");
    }

    @Test
    void loadFaculty() {
        // Branch 1: Test what happens when the Database HAS data

        // Arrange: Tell the fake repository what to return
        when(facultyRepository.findAll()).thenReturn(List.of(mockFacultyEntity));
        when(reviewRepository.findByFaculty_Id(1L)).thenReturn(Collections.emptyList());

        // Act: Run the method
        facultyService.loadFaculty();
        List<Faculty> loadedFaculty = facultyService.findAll();

        // Assert: Verify the expected outcomes
        assertFalse(loadedFaculty.isEmpty(), "Faculty list should not be empty");
        assertEquals(1, loadedFaculty.size(), "Should load exactly 1 faculty member");
        assertEquals("Dr. Ahmad Raza Shahid", loadedFaculty.get(0).getName());
    }

    @Test
    void findAll() {
        // Arrange: Populate the service with our mock data first
        when(facultyRepository.findAll()).thenReturn(List.of(mockFacultyEntity));
        facultyService.loadFaculty();

        // Act
        List<Faculty> result = facultyService.findAll();

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void search() {
        // Arrange: Populate the list so we have something to search
        when(facultyRepository.findAll()).thenReturn(List.of(mockFacultyEntity));
        facultyService.loadFaculty();

        // Act & Assert - Branch 1: Search matches Name perfectly
        List<Faculty> result1 = facultyService.search("Ahmad", "All");
        assertEquals(1, result1.size(), "Should find Dr. Ahmad");

        // Act & Assert - Branch 2: Search matches Department
        List<Faculty> result2 = facultyService.search(null, "CS");
        assertEquals(1, result2.size(), "Should find CS faculty");

        // Act & Assert - Branch 3: Search does NOT match anything (Error Guessing)
        List<Faculty> result3 = facultyService.search("Batman", "Math");
        assertTrue(result3.isEmpty(), "Should return empty list for no matches");
    }

    @Test
    void findById() {
        // Branch 1: Faculty IS found in the database

        // Arrange
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(mockFacultyEntity));

        // Act
        Optional<Faculty> result = facultyService.findById(1L);

        // Assert
        assertTrue(result.isPresent(), "Should find the faculty member");
        assertEquals("Dr. Ahmad Raza Shahid", result.get().getName());
    }


}