package com.caophuc.court.controller;


import com.caophuc.court.dto.*;
import com.caophuc.court.model.CourtImage;
import com.caophuc.court.service.CourtImageService;
import com.caophuc.court.service.CourtService;
import com.caophuc.court.service.ReviewService;
import com.caophuc.court.util.SportType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
@Tag(name = "Court Management", description = "APIs for managing courts and reviews")
public class CourtController {

    private final CourtService courtService;
    private final CourtImageService courtImageService;
    private final ReviewService reviewService;

    // --- Court CRUD & Search (Dùng CourtService) ---
    @Operation(summary = "Create a new court", description = "Requires ROLE_ADMIN or ROLE_MANAGER")
    @PostMapping
    public ResponseEntity<CourtDetailResponse> create(
            @RequestBody CreateCourtRequest req,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        if (userId == null || userRole == null || (!userRole.equals("ROLE_ADMIN") && !userRole.equals("ROLE_MANAGER"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return new ResponseEntity<>(courtService.createCourt(req, userId), HttpStatus.CREATED);
    }

    @Operation(summary = "Get court details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CourtDetailResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(courtService.getCourtById(id));
    }

    @Operation(summary = "Search and filter courts")
    @GetMapping("/search")
    public ResponseEntity<Page<CourtSummaryResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SportType courtType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusInKm,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        CourtSearchCriteria criteria = new CourtSearchCriteria();
        criteria.setName(name); criteria.setCourtType(courtType);
        criteria.setMinPrice(minPrice); criteria.setMaxPrice(maxPrice);
        criteria.setMinRating(minRating);
        criteria.setLatitude(latitude); criteria.setLongitude(longitude);
        criteria.setRadiusInKm(radiusInKm);
        return ResponseEntity.ok(courtService.searchCourts(criteria, pageable));
    }

    @Operation(summary = "Update an existing court", description = "Requires ROLE_ADMIN or ROLE_MANAGER")
    @PutMapping("/{id}")
    public ResponseEntity<CourtDetailResponse> updateCourt(
            @PathVariable Integer id,
            @RequestBody CreateCourtRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (userId == null || userRole == null || (!userRole.equals("ROLE_ADMIN") && !userRole.equals("ROLE_MANAGER"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(courtService.updateCourt(id, request, userId));
    }

    @Operation(summary = "Delete a court", description = "Requires ROLE_ADMIN or ROLE_MANAGER")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourt(
            @PathVariable Integer id,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (userId == null || userRole == null || (!userRole.equals("ROLE_ADMIN") && !userRole.equals("ROLE_MANAGER"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        courtService.deleteCourt(id, userId);
        return ResponseEntity.noContent().build();
    }

    // --- Review (Dùng ReviewService) ---
    @Operation(summary = "Add a review to a court", description = "Requires user authentication")
    @PostMapping("/{courtId}/reviews")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Integer courtId,
            @RequestBody CreateReviewRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return new ResponseEntity<>(reviewService.addReview(courtId, request, userId), HttpStatus.CREATED);
    }

    @Operation(summary = "Get reviews for a court")
    @GetMapping("/{courtId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable Integer courtId,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviews(courtId, pageable));
    }


    // --- Image Management (Dùng CourtImageService) ---
    @Operation(summary = "Upload an image for a court", description = "Requires ROLE_ADMIN or ROLE_MANAGER")
    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<CourtImage> uploadImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (userId == null || userRole == null || (!userRole.equals("ROLE_ADMIN") && !userRole.equals("ROLE_MANAGER"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CourtImage newImage = courtImageService.uploadCourtImage(id, file, userId);
        return new ResponseEntity<>(newImage, HttpStatus.CREATED);
    }

    @Operation(summary = "Delete a court image", description = "Requires ROLE_ADMIN or ROLE_MANAGER")
    @DeleteMapping("/{courtId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Integer courtId,
            @PathVariable Integer imageId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (userId == null || userRole == null || (!userRole.equals("ROLE_ADMIN") && !userRole.equals("ROLE_MANAGER"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        courtImageService.deleteCourtImage(courtId, imageId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set a court image as thumbnail", description = "Requires ROLE_ADMIN or ROLE_MANAGER")
    @PutMapping("/{courtId}/thumbnail/{imageId}")
    public ResponseEntity<Void> setThumbnail(
            @PathVariable Integer courtId,
            @PathVariable Integer imageId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (userId == null || userRole == null || (!userRole.equals("ROLE_ADMIN") && !userRole.equals("ROLE_MANAGER"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        courtImageService.setThumbnail(courtId, imageId, userId);
        return ResponseEntity.ok().build();
    }
}