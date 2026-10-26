# OOP Ex1 – Plane Projection of a 3D Volume

PRD and SRS · Version 1.2 (demo, CSV input, test data and the assignment's plane definition added)

## Legend

- **\[A\]** = stated in the assignment. **\[D\]** = a design decision made here, because the assignment leaves the point open.
- **FR** = Functional Requirement (what the program does). **NFR** = Non-Functional Requirement (how well it does it: speed, robustness, code quality). The letter after the dash is the topic and the number is a counter, so FR-U1 is the first rule about unsigned values.
- Topics: **FR-N** NRRD loading · **FR-U** Unsigned values · **FR-G** Geometry (the plane) · **FR-S** Sampling · **FR-D** Demo · **NFR-P** Performance · **NFR-R** Robustness · **NFR-M** Maintainability.

| Term | Meaning |
| --- | --- |
| M | the 3D volume (matrix) of size sx*sy*sz |
| E | the 2D output image, W columns by H rows |
| a, b, c | the three points of a triangle plane |
| a1, a2, b, c | the four points of a trapezoid plane |
| Dab, Dbc | Dab = dist(a,b) for a triangle, dist(a1,b) for four points; Dbc = dist(b,c) |
| L | leg length, equal to Dab |
| EPS | 0.001: the tolerance given in the assignment for checking a plane \[A\] |
| TINY | 1e-9: a length below this counts as zero, so we never divide by it |
| Voxel space | 3D coordinates where voxel (i,j,k) is centred at (i,j,k) |
| Frame | one image of the output video |

## Part I – Product Requirements (PRD)

### 1. Purpose and problem

Build a robust, fast Java engine that takes a 3D volume and a plane and returns the 2D image of that plane through the volume, like the X/Y/Z views in 3D Slicer but for any plane. A demo program applies it to a list of planes and writes the images as an MP4 video. The assignment grades design and code quality first; the algorithm is the vehicle. Its algorithm description is intentionally incomplete (rounding, orientation, degenerate input), so this document settles those gaps. \[A\]

- **Input data:** volumetric MRI datasets in NRRD format from the 3D Slicer project. \[A\]
- **Input plane:** a plane in 3D voxel space, given either as three points `(a,b,c)` forming an isosceles triangle with `|dist(a,b) - dist(a,c)| < EPS`, or as four points `(a1,a2,b,c)` forming an isosceles trapezoid with `|dist(a1,b) - dist(a2,c)| < EPS`, `dist(a1,a2) <= dist(b,c)`, and the legs `a1→b` and `a2→c` in one plane. \[A\]
- **Output:** a 2D matrix (image) `E` of size about `Dab*Dbc`, where `Dab` is the leg length and `Dbc=dist(b,c)`, holding the voxel intensities sampled along the plane. A sampled point outside the volume gives `0`. \[A\]
- **Demo:** a `main` program run as `file.nrrd file.csv file.mp4`. The CSV lists the planes, and the MP4 gets one frame per plane, in the order of the file. \[A\]

### 2. Goals and non-goals

1. Correct output for any valid plane, including planes partly or fully outside the volume.
2. At least 30 FPS (must) and 100+ FPS (expected) on a 500³ byte volume. \[A\]
3. A clean object-oriented design, documented before coding.
4. Stability: no crashes, hangs or silent corruption on any input.
5. A working demo and a short experiment with its video, described in the README. \[A\]

Non-goals (v1): GUI; interpolation other than nearest-neighbour; physical (millimetre) coordinates from NRRD `space directions`; floating-point voxel types; tuning of video quality or compression.

### 3. Success metrics, assumptions and risks

- Success: all tests in SRS section 5 pass, the benchmark meets NFR-P1, the robustness inputs never crash the program, and the demo produces a playable MP4.
- Platform: Java 17 or later. The projection engine uses the standard library only; the demo may use an MP4 encoder library (SRS decision 6). \[D\]
- Memory: a 1023³ volume needs about 1.07 GB (8-bit), 2.14 GB (16-bit) or 4.28 GB (32-bit), so start the JVM with enough `-Xmx`. \[D\]
- Risk: a plane with points far outside the volume could request a huge output. Mitigation: FR-G6.
- Risk: the MRI file used here has type short (signed 16-bit). Mitigation: the opt-in in FR-N4, with a check for negative values.
- Risk: the CSV format is only loosely described. Mitigation: SRS decision 5; only one class depends on it.
- Risk: Java has no built-in MP4 writer. Mitigation: SRS decision 6; the encoder sits behind an interface.
- Risk: JIT warm-up distorts FPS numbers. Mitigation: protocol in NFR-P1.

## Part II – Software Requirements (SRS)

### 1. Inputs and outputs

- **Volume M:** size `sx*sy*sz` with `1 <= sx,sy,sz < 2^10` (at most 1023); entries are unsigned integers of 8, 16 or 32 bits. \[A\]
- **Plane:** a triangle `(a,b,c)` or a trapezoid `(a1,a2,b,c)`, with coordinates in voxel space. \[A\]\[D\]
- **Image E:** width `W` and height `H` (computed in 2.4), indexed `E[x][y]`; pixels with no valid source voxel are `0`. \[A\]
- **Demo input and output:** an NRRD file, a CSV file of planes (2.6) and an MP4 file. \[A\]

### 2. Functional requirements

#### 2.1 NRRD loading

- **FR-N1** Read the NRRD header (magic line, `key: value` fields, blank line) and the binary data. \[A\]
- **FR-N2** Supported: `dimension: 3`; `type` uint8/uchar, uint16/ushort, uint32/uint (and their long names); `encoding` raw or gzip; `endian` little or big. Detached data (`.nhdr`) is not supported. \[D\]
- **FR-N3** `sizes[0]` is `sx` and varies fastest in memory, then `sy`, then `sz`; linear index = `x + sx*(y + sy*z)`. \[D\]
- **FR-N4** Signed types (int8, int16, int32 and their other names, such as short) are rejected with `NrrdFormatException` by default. With an explicit opt-in (allowSigned) they are accepted only if no voxel is negative (sign bit set); otherwise `NrrdFormatException`, so no value is silently misread. \[D\]
- **FR-N5** Any malformed, truncated or unsupported file, sizes outside `[1,1023]`, or data length different from `sx*sy*sz*bytesPerVoxel` raises `NrrdFormatException`. \[D\]

#### 2.2 Unsigned values

- **FR-U1** Read values as unsigned with `Byte.toUnsignedInt`, `Short.toUnsignedInt`, `Integer.toUnsignedLong` (or masks): stored `0xFF`, `0xFFFF`, `0xFFFFFFFF` read as 255, 65535, 4294967295. \[A\]
- **FR-U2** The image stores raw bits in an `int[]`; `get(x,y)` returns a `long` via `Integer.toUnsignedLong`. \[D\]

#### 2.3 The plane

- **FR-G1** Triangle `(a,b,c)`: require `|dist(a,b) - dist(a,c)| < EPS`. \[A\]
- **FR-G2** Four points `(a1,a2,b,c)`: require (a) `|dist(a1,b) - dist(a2,c)| < EPS`, (b) `dist(a1,a2) <= dist(b,c)`, and (c) `a1→b` and `a2→c` lie in one plane, checked as: the distance from `a2` to the plane through `a1`, `b`, `c` is below `EPS`. If `a1` equals `a2` the plane is a triangle. \[A\]\[D\]
- **FR-G3** An invalid plane throws `PlaneDefinitionException` at construction: NaN or Infinity coordinates, a failed check of FR-G1 or FR-G2, `L<TINY`, `Dbc<TINY`, or collinear points (the cross product `(b-a1) x (c-a1)` is shorter than `TINY`). \[D\]
- **FR-G4** A valid plane partly or fully outside the volume is not an error; outside pixels are `0`. \[A\]
- **FR-G5** The output width is based on `Dbc = dist(b,c)`. By FR-G2(b) this is the longer base, so rows inside the plane are never wider than the image (the extra last row of 2.4 can be; the guard drops what does not fit). \[A\]
- **FR-G6** `W` and `H` must not exceed `MAX_OUTPUT_DIM = 4096` (configurable), otherwise `PlaneDefinitionException`. \[D\]

#### 2.4 Projection algorithm

The assignment's algorithm is the reference; this is it stated precisely.

- **Normalise.** Triangle: `start1=start2=a`, `L=Dab`, `dir1=(b-a)/dist(a,b)`, `dir2=(c-a)/dist(a,c)` (unit vectors). Four points: `start1=a1`, `start2=a2`, `L=dist(a1,b)`, `dir1=(b-a1)/dist(a1,b)`, `dir2=(c-a2)/dist(a2,c)`. \[A\]\[D\]
- **Output size.** `H = max(1, ceil(L))`, `W = max(1, ceil(Dbc))`; `E` starts all `0`. \[D\]

The row loop is written out step by step in Appendix A at the end of this document; the notes below explain its key points.

- Row `y` is sampled at distance `y+1` from the start, because the assignment steps `a1`, `a2` before sampling; kept for conformance. So the last row is at distance ceil(L), which can lie up to 1 unit beyond the far edge when L is not a whole number. It is still sampled, exactly as in the assignment: points outside the volume give 0 (FR-S2) and the guard keeps writes inside the image. \[A\]
- The `x1` loop is inclusive (`floor(d12)+1` samples per row) and the guard `0<=x<W` drops an extra edge sample. If `d12 < TINY` the row holds one sample and no division happens: this is the guard for `dp = vector(a1,a2)/d12`. \[A\]\[D\]
- `double` arithmetic over at most 4096 steps keeps the accumulated error below about 1e-8, which only matters exactly on a rounding boundary (FR-S1).

#### 2.5 Sampling

- **FR-S1** Nearest neighbour with one rounding rule: `i = floor(px + 0.5)`, likewise `j`, `k`. Truncating with `(int)` is not allowed (it maps `-0.7` to `0`). \[A\]\[D\]
- **FR-S2** The bounds check uses the rounded integers (`0<=i<sx`, `0<=j<sy`, `0<=k<sz`), otherwise the result is `0`. For `sx=500`: `-0.5` inside, `-0.51` outside, `499.49` inside, `499.5` outside. \[A\]\[D\]

#### 2.6 Demo

- **FR-D1** The program is started as `Ex1 file.nrrd file.csv file.mp4` (the assignment shows `python Ex0.py file.nrrd file.csv file.mp4`). Any other number of arguments prints a usage message and ends with a non-zero exit code. \[A\]\[D\]
- **FR-D2** CSV format: a text file with one plane per line. A line has 9 numbers (three points `a,b,c`) or 12 numbers (four points `a1,a2,b,c`); each point is `x,y,z` in voxel space; all numbers are separated by commas, spaces around them are allowed; empty lines are ignored; a first line that is not numeric is skipped as a header. \[D\]
- **FR-D3** A line with any other number of values, a value that is not a number, or an invalid plane stops the program with a message that names the line number and the reason; no video is written. A CSV without any plane line is also an error. \[D\]
- **FR-D4** The video has one frame per plane line, in the order of the file. \[A\]
- **FR-D5** Gray conversion: the minimum and maximum voxel value of the whole volume are found once; a pixel `v` becomes `round(255*(v-min)/(max-min))`, clamped to `[0,255]` (computed in `long`, so 32-bit data works); if `max == min` every pixel is `0`. \[D\]
- **FR-D6** All video frames must have one size, but planes differ in size. Frame width = the largest `W`, frame height = the largest `H` over all planes, each rounded up to an even number (video encoders commonly need this); each image is placed in the centre of a black frame. \[D\]
- **FR-D7** The output is an MP4 file with 10 frames per second. \[D\]
- **FR-D8** Video writing is behind an interface, so the encoder can be replaced without touching the projection engine. \[D\]
- **FR-D9** Exit code `0` on success; on any failure (missing file, bad NRRD, bad CSV) a readable message and a non-zero exit code. \[D\]

### 3. Non-functional requirements

- **NFR-P1** At least 30 FPS (must), 100+ FPS (expected) on a 500³ byte volume. \[A\] A frame is one `project()` call into a preallocated buffer, without display, file loading or video encoding. Protocol \[D\]: 1000 warm-up frames discarded, 1000 measured, report median and worst 1% frame time, CPU, cores, JVM and thread count; planes tested: axis-aligned, 45° oblique, and one cutting along the slowest memory axis; output about `500*500`.
- **NFR-P2** No object allocation in the inner loops (verify: zero thread-allocated bytes over 1000 frames). Flat primitive arrays, loop invariants hoisted, no virtual call per voxel; a specialised loop per voxel type is allowed. \[A\]\[D\]
- **NFR-R1** For any accepted input `project()` never throws or hangs; invalid input fails fast with the documented exceptions at load or construction time. \[A\]\[D\]
- **NFR-R2** Deterministic output on every run. The largest linear index (1023³ - 1 = 1,070,599,166) fits an `int`, so single Java arrays suffice. \[D\]
- **NFR-M1** Immutable data types, single-responsibility classes, Javadoc on the public API, unit tests per class.

### 4. Design outline \[D\]

The full design is in the Design Document. In short:

- Engine: `Volume` (with `ByteVolume`, `ShortVolume`, `IntVolume`), `NrrdReader`, `PlaneDefinition` (`TrianglePlane`, `TrapezoidPlane`), `ProjectionImage`, `Projector`.
- Demo: `Ex1` (main), `PlaneCsvReader`, `GrayscaleMapper`, and the interface `VideoWriter` with one MP4 implementation.
- The engine never depends on the demo classes or on the encoder library.

### 5. Test data and testing strategy

**Data set of NRRD files.** \[A\]\[D\]

- `MRHead.nrrd`: the real MRI file from the assignment; used for the demo and the experiment (sizes 256 256 130, type short, encoding gzip; so it needs the gzip reader and the signed opt-in).
- Synthetic valid files, created by the test code: constant 255 (8-bit, raw); coordinates encoded in the value (16-bit, gzip, little endian); values above 2^31 (32-bit, big endian); `1*1*1` and `1*N*1` volumes.
- Invalid files: truncated data, `dimension: 2`, unknown `type`, sizes `0` and `1024`, a signed file with a negative voxel.
- CSV files: only 3-point lines; only 4-point lines; a mix with a header line; a plane fully outside the volume; a bad value count; an invalid plane; an empty file.

**Strategy.**

- Unit tests for every class (reader, planes, sampling, CSV reader, gray mapper).
- A reference test: the optimised `Projector` against a slow implementation written straight from Appendix A.
- An end-to-end demo test: run `Ex1` on a small synthetic volume and CSV, then check the exit code, that the MP4 exists and is not empty, and that its frame count equals the number of plane lines (read back with the encoder library or `ffprobe`).
- Error tests for the demo: wrong argument count, missing file, bad CSV line.
- A separate benchmark for NFR-P1; the demo is not used for speed numbers.
- A manual check: play the MP4 from `MRHead.nrrd` and look at it.

**Acceptance tests.**

1. **Constant volume:** all voxels 255, plane inside; written pixels are 255, pixels the algorithm does not write stay 0.
2. **Unsigned values:** `0xFF`, `0xFFFF`, `0xFFFFFFFF` read as 255, 65535, 4294967295.
3. **Plane fully outside:** all pixels 0, no exception.
4. **Rounding boundaries** (`sx=500`): `-0.5`, `-0.51`, `499.49`, `499.5`, `-0.7` give inside, outside, inside, outside, outside.
5. **Brute-force equivalence:** on random volumes and valid planes (including axis-aligned slices of a volume that encodes its own coordinates) the optimised `Projector` equals the slow reference.
6. **Invalid planes:** collinear points, `b=c`, `a=b`, NaN, Infinity, `|dist(a,b)-dist(a,c)| >= EPS`, `dist(a1,a2) > dist(b,c)`, `a2` further than `EPS` from the plane, `ceil(L) > 4096` each throw `PlaneDefinitionException` at construction and nothing else.
7. **Tiny volumes and trapezoids:** `1*1*1` and `1*N*1` volumes; a rectangle and a true isosceles trapezoid give the expected width and centred rows; a leg length of 10.4 gives 11 rows, the last one sampled at distance 11, as in the assignment.
8. **NRRD:** raw and gzip, little and big endian, all three unsigned types load; truncated data, wrong `dimension`, unknown `type` or `encoding`, sizes `0` or `>=1024`, signed types without the option, and signed files with a negative voxel each give the documented error; a signed file with only non-negative values loads when the option is on.
9. **Allocation and determinism:** zero bytes allocated over 1000 frames; single-threaded and multi-threaded runs give identical images.
10. **Performance:** NFR-P1 benchmark reports at least 30 FPS on all three planes, target 100+.
11. **CSV reading:** 9 and 12 value lines, header line, empty lines, spaces; wrong counts, text instead of numbers, and an empty file give an error naming the line.
12. **Gray mapping:** min maps to 0, max to 255, a value below min is clamped to 0, `max == min` gives 0, 32-bit values above 2^31 map correctly.
13. **Demo end to end:** the check described in the strategy, with a mixed CSV; frame size is even and equal for all frames; error cases give a non-zero exit code and no video.

### 6. Decisions to confirm

These points are unclear or missing in the assignment. Each one is decided here, with the reason, so you can explain and defend the choice. All of them are isolated in the code, so changing one later is cheap.

1. **Signed voxel data (decided).** Your file has `type: short`, which means signed 16-bit numbers. The assignment says unsigned. Decision: use the opt-in of FR-N4. The loader checks every voxel. If none is negative, the values are read as unsigned and are exactly the same numbers. If a negative voxel exists, loading stops with an error instead of showing wrong values.
2. **Rows beyond the far edge (decided).** Example: if `Dab` is 10.4, the loop makes 11 rows, and row 11 is sampled at distance 11, which is 0.6 outside the triangle. Decision: follow the assignment and sample that row too. Reason: the assignment's loop is the reference, and any expected output was most likely made with it. Points outside the volume give `0`, and the check `0<=x<W` keeps all writes inside the image. The assignment also says that only points within the triangle are projected, but its loop samples this row too; we follow the loop. \[D\]
3. **Trapezoid width (decided by the assignment).** Its Q&A requires `dist(a1,a2) <= dist(b,c)`, so `dist(b,c)` is always the longer base and gives the width.
4. **Output cap (decided).** `MAX_OUTPUT_DIM = 4096` only protects against asking for a huge image by mistake.
5. **CSV format (decided).** The assignment says only that the file is a list of 3 or 4 point coordinates, with one video frame for each plane in the order of the list. Decision: each plane is one line. Reason: a plane needs exactly 3 or 4 points, and a line is the natural unit of a list in a CSV file; with 9 or 12 numbers per line the program can tell the two cases apart just by counting. The reader also accepts a header line and spaces. If the real files look different, only `PlaneCsvReader` changes.
6. **MP4 encoder (decided).** Java has no built-in MP4 writer. Decision: a pure-Java library (for example JCodec), added with Maven or Gradle. Reason: anyone who clones the repository can build and run it without installing other programs, while `ffmpeg` would have to be installed on every computer. The interface `VideoWriter` keeps the choice in one class. Fallback if the library does not work on your machine: a second `VideoWriter` that calls `ffmpeg`. Check this early, before writing the rest of the demo.
7. **Video details (decided).** Gray scaling by the volume's min and max (every frame has the same brightness scale, so planes can be compared); a black frame sized to the largest plane (a video needs one frame size); 10 FPS (slow enough to see each plane). They affect only how the video looks.

Copy these decisions, with their reasons, into the Design Decisions section of the README.

## Appendix A – Reference pseudo-code for 2.4

Pseudo-code, not Java. The slow reference implementation used in test 5 is written straight from it.

```
a1 = start1; a2 = start2
for y = 0 .. H-1:
    a1 += dir1; a2 += dir2          // as in the assignment: step first
    d12 = dist(a1, a2)
    dp  = (d12 < TINY) ? (0,0,0) : (a2 - a1) / d12
    x0  = round(W/2 - d12/2)        // [D] centres the row
    p   = a1
    for x1 = 0 .. floor(d12):       // assignment: x1 <= d12
        x = x0 + x1
        if (0 <= x < W) E[x][y] = sample(M, p)
        p += dp
return E
```

## Appendix B – Team guide for beginners

This appendix explains the terms, the first practical steps and the reasons behind some decisions, so that every team member can follow the project without outside help.

### B1. What the documents are for

- **PRD** (Product Requirements Document): what we build and why.
- **SRS** (Software Requirements Specification): exactly what the program must do. If code and SRS disagree, one of them is wrong.
- **Design Document**: how the code is organised into classes.
- **README**: the front page of the repository; it links to everything else.

### B2. Glossary

- **Voxel:** a pixel in 3D. One number inside the volume `M`.
- **Volume:** the whole 3D block of voxels (here: an MRI scan), `sx*sy*sz` numbers.
- **NRRD:** a file format for 3D images. It has a readable text header, then the numbers in binary form.
- **Unsigned / signed:** unsigned numbers are never negative (8-bit: 0 to 255; 16-bit: 0 to 65,535; 32-bit: 0 to 4,294,967,295). Signed 16-bit numbers go from -32,768 to 32,767. Java has no unsigned types, so we convert with `Byte.toUnsignedInt` and similar methods.
- **Raw / gzip:** the binary data is stored as is, or compressed.
- **Endian (little or big):** the order of the bytes inside a number that is larger than 8 bits. The header says which one the file uses.
- **Isosceles triangle:** two sides have the same length (here `ab` and `ac`).
- **Isosceles trapezoid:** two parallel sides (the bases) and two legs of the same length.
- **Plane and projection (here):** we cut the volume along a flat surface and show the voxel values on it as a 2D image.
- **Nearest neighbour:** a point between voxels takes the value of the closest voxel.
- **EPS (0.001):** the computer cannot store most decimals exactly, so two lengths count as equal if they differ by less than EPS.
- **FPS (frames per second):** here, how many projections the engine can compute in one second.
- **JIT warm-up:** Java makes code faster after it has run for a while. The first runs are slower, so speed is measured after a warm-up.
- **Pseudo-code:** a step-by-step description that looks like code but is not a real language.
- **Fail fast:** check input as early as possible and stop with a clear message, instead of continuing with wrong data.
- **Exit code:** the number a program returns when it ends. `0` means success; anything else means an error.
- **Frame:** one image of the output video.

### B3. How to check the volume file

The header of an NRRD file is plain text at the top of the file, and the binary data follows it.

1. Open the file in VS Code. If a message says the file is binary, choose Open Anyway.
2. The header ends at the first empty line. Everything after it looks like random characters; ignore it.
3. Find these lines:
   - `sizes:` the three sizes `sx sy sz`.
   - `type:` the type of the numbers, for example `short` (signed 16-bit) or `unsigned char` (unsigned 8-bit).
   - `encoding:` `raw` or `gzip`.
   - `endian:` `little` or `big` (not written for 8-bit data).

Example of a header. The lines type, sizes, encoding and endian are the values found in our MRI file; the other lines show the usual layout (the version in the first line may differ):

```
NRRD0004
type: short
dimension: 3
sizes: 256 256 130
endian: little
encoding: gzip
```

### B4. Why the loader checks for negative numbers

The assignment says the voxels are unsigned, but a file with `type: short` is signed. If we read a signed `-1` as unsigned, it becomes 65,535, and the picture would be wrong without any warning. So the loader (with the opt-in of FR-N4) checks every voxel:

- If no voxel is negative, the unsigned reading gives exactly the same numbers, and loading continues.
- If a voxel is negative, loading stops with an error that says so.

The alternatives are to set negative values to 0 or to shift all values up. Both change the data silently, so we did not choose them. MRI scans normally have no negative values, so the check is expected never to fire; it is there for safety.

### B5. Suggested working order

Commit after every step, with a short message that says what was done.

1. **Setup:** install JDK 17 or later, add the Java extensions to VS Code, create the project, and check that a test class can run.
2. **Volume and loading:** `Volume`, its three implementations and `NrrdReader`, with tests on small synthetic files and then on `MRHead.nrrd`.
3. **Planes:** `TrianglePlane`, `TrapezoidPlane` and their tests (valid and invalid planes).
4. **Projection:** first a slow, simple `Projector` written straight from Appendix A, with tests. Then the fast version, checked against the slow one on random inputs.
5. **Demo parts:** `PlaneCsvReader`, `GrayscaleMapper`, `VideoWriter` and `DemoRunner`.
6. **Demo end to end:** `Ex1`, the demo test, and the first video.
7. **Speed and experiment:** the benchmark of NFR-P1, then the experiment described in the README.

A possible split between two people: one takes steps 2 and 5 (files and data), the other takes steps 3 and 4 (geometry and projection); both meet for step 6. Agree on the interfaces of `Volume` and `PlaneDefinition` first, so that the two parts fit together.

### B6. Questions the team may ask

- **Why is there pseudo-code in a requirements document?** The assignment's algorithm is the requirement itself, and it is incomplete. The pseudo-code shows exactly how we closed the gaps. It sits in an appendix so the main text stays about requirements.
- **Why a slow and a fast projector?** The slow one is easy to read and easy to trust. The fast one is checked against it, so speed work cannot silently break correctness.
- **Why must the engine not depend on the demo or on the video library?** So the engine can be tested and reused alone, and so changing the video library touches one class only.
- **What if the instructor's CSV files look different?** Only `PlaneCsvReader` changes (decision 5).
- **What if the video library does not work on a computer?** Use the fallback of decision 6: a second `VideoWriter` that calls `ffmpeg`.
