# OOP-Ex1-3D-Volume-Projection
## Problem Definition
### Overview
The primary problem is to design and implement an algorithm that computes a 2D cross-sectional image (projection) from a 3D volumetric dataset along any arbitrary plane. Because the base problem statement is intentionally under-specified for direct coding, a structured software design is required prior to implementation to ensure a robust, runtime-efficient solution.

# OOP-Ex1-3D-Volume-Projection

## 1. Product Overview (PRD)
* **What are we building?** A high-performance 2D cross-sectional projection engine that slices 3D volumetric datasets (NRRD format) along any arbitrary 3D plane.
* **For whom and why?** Designed for medical imaging and volumetric analysis workflows (such as inspecting 3D Slicer MRI scans), where interactive, real-time slicing (30–100+ FPS) is required without the overhead of full 3D volume rendering.

---

## 2. Software Requirements Specification (SRS)
*Answering: Exactly what must the system do?*

### 2.1 Problem Definition (Inputs & Outputs)
* **Input 1 — 3D Volumetric Matrix (`M`):** A 3D raster matrix of size `sx * sy * sz` loaded from an NRRD file.
  * **Dimensions:** `1 <= sx, sy, sz < 2^10`, with voxel coordinates bounded by `0 <= x < sx`, `0 <= y < sy`, and `0 <= z < sz`.
  * **Data Types:** Each voxel entry `M[x][y][z]` is an unsigned integer of `8`, `16`, or `32` bits.
* **Input 2 — Projection Plane (`p`):** A geometric plane in 3D space defined by either:
  * Three 3D points `(a, b, c)` forming an isosceles triangle where `dist(a,b) = dist(a,c)`.
  * Two equal-length coplanar 3D vectors (`a' -> b` and `a'' -> c`) forming an isosceles trapezoid.
* **Output — 2D Projected Matrix (`E`):** A 2D raster matrix of dimensions `Dab * Dbc` containing the sampled voxel values along the plane `p`.

### 2.2 Functional Requirements
* **NRRD Data Parsing:** Parse header metadata and raw binary voxel data from NRRD volumetric files.
* **Unsigned Integer Handling:** Accurately process 8, 16, and 32-bit unsigned values in Java without sign-extension corruption.
* **Arbitrary Plane Sampling:** Compute 2D projections for both triangle and trapezoid plane definitions.
* **Out-of-Bounds Safety:** Any sampled 3D coordinate falling outside `[0, sx) x [0, sy) x [0, sz)` must evaluate to `0`.
* **Discrete Index Resolution:** Safely map continuous floating-point 3D coordinates to discrete integer matrix indices and guard against division-by-zero edge cases (e.g., when `d12 = 0`).

### 2.3 Non-Functional Requirements (Performance & Stability)
* **Minimum Frame Rate (Must):** At least **30 FPS** on a `500^3` byte matrix.
* **Target Frame Rate (Expected):** **100+ FPS** on a `500^3` byte matrix.
* **Stability:** Zero crashes on degenerate geometric inputs or out-of-bounds slicing planes.

# OOP-Ex1-3D-Volume-Projection


## 1. Problem Definition

### Overview
The primary problem is to design and implement a robust, runtime-efficient algorithm that computes a 2D cross-sectional image (projection) from a 3D volumetric dataset along any arbitrary plane. Because the base problem statement is intentionally under-specified for direct coding, a structured software design is required prior to implementation.

### Inputs
* **3D Volumetric Data Matrix (M):** A 3D raster matrix of size `sx * sy * sz` loaded from an NRRD format file (such as medical MRI data from 3D Slicer).
  * **Dimensions:** `1 <= sx, sy, sz < 2^10`, with coordinates bounded by `0 <= x < sx`, `0 <= y < sy`, and `0 <= z < sz`.
  * **Data Types:** Each voxel entry `M[x][y][z]` is an unsigned integer of 8, 16, or 32 bits.
* **Projection Plane (p):** A geometric plane in 3D space defined by three 3D points `(a, b, c)` forming an isosceles triangle where `dist(a,b) = dist(a,c)`, or alternatively an isosceles trapezoid defined by two equal-length coplanar vectors (`a' -> b` and `a'' -> c`).

### Output
* **2D Projected Matrix (E):** A 2D matrix of size `Dab * Dbc` containing the sampled voxel intensities along the specified plane.

---

## 2. Project Scope & SRS (Software Requirements Specification)

### Functional Requirements
- **Data Parsing:** Reading and handling 3D NRRD (MRI) volumetric datasets.
- **Unsigned Data Type Support (Java):** Properly converting raw 8, 16, and 32-bit unsigned voxel values into unsigned representations in Java (using bitwise masking / `Byte.toUnsignedInt`, `Short.toUnsignedInt`, and `Integer.toUnsignedLong`) to prevent sign-extension corruption.
- **Core Algorithm:** Executing the scanline plane projection algorithm across both isosceles triangle and isosceles trapezoid plane representations.
- **Boundary Handling:** Any sampled point `p` falling outside the bounds of the 3D matrix `M` must be assigned a value of `0`.
- **Continuous-to-Discrete Mapping:** Resolving floating-point 3D coordinates into valid integer indices for matrices `M` and `E` (using Nearest-Neighbor rounding/truncation) while preventing division-by-zero when `d12 = 0`.

### Non-Functional Requirements (Performance)
* **Language:** Java.
* **Minimum Threshold (Must):** At least **30 FPS** runtime performance on a `500^3` byte matrix.
* **Target Benchmark:** **100+ FPS** on a `500^3` byte matrix (utilizing flat/cache-friendly memory access and zero object allocations inside the inner loop).