# OOP Ex1 – Plane Projection of a 3D Volume

PRD and SRS · Version 1.1 (with legend)

## Legend

- **\[A\]** = stated in the assignment. **\[D\]** = a design decision made here, because the assignment leaves the point open.
- **FR** = Functional Requirement (what the program does). **NFR** = Non-Functional Requirement (how well it does it: speed, robustness, code quality). The letter after the dash is the topic and the number is a counter, so FR-U1 is the first rule about unsigned values.
- Topics: **FR-N** NRRD loading · **FR-U** Unsigned values · **FR-G** Geometry (the plane) · **FR-S** Sampling · **NFR-P** Performance · **NFR-R** Robustness · **NFR-M** Maintainability.

| Term | Meaning |
| --- | --- |
| M | the 3D volume (matrix) of size sx*sy*sz |
| E | the 2D output image, W columns by H rows |
| a, b, c | the three points of the plane (triangle case) |
| Dab, Dbc | dist(a,b) and dist(b,c) |
| L | leg length (Dab for a triangle) |
| EPS | 1e-9: a length below this counts as zero |
| REL\_TOL | 1e-6: tolerance when comparing two lengths |
| Voxel space | 3D coordinates where voxel (i,j,k) is centred at (i,j,k) |

## Part I – Product Requirements (PRD)

### 1. Purpose and problem

Build a robust, fast Java engine that takes a 3D volume and a plane and returns the 2D image of that plane through the volume, like the X/Y/Z views in 3D Slicer but for any plane. The assignment grades design and code quality first; the algorithm is the vehicle. Its algorithm description is intentionally incomplete (rounding, orientation, degenerate input), so this document settles those gaps. \[A\]

- **Input data:** volumetric MRI datasets in NRRD format from the 3D Slicer project. \[A\]
- **Input plane:** a plane in 3D voxel space, given either as three points `(a,b,c)` forming an isosceles triangle with `dist(a,b)=dist(a,c)`, or as two equal-length coplanar vectors `a'→b` and `a''→c` forming an isosceles trapezoid. \[A\]
- **Output:** a 2D matrix (image) `E` of size about `Dab*Dbc`, where `Dab=dist(a,b)` and `Dbc=dist(b,c)`, holding the voxel intensities sampled along the plane. A sampled point outside the volume gives `0`. \[A\]

### 2. Goals and non-goals

1. Correct output for any valid plane, including planes partly or fully outside the volume.
2. At least 30 FPS (must) and 100+ FPS (expected) on a 500³ byte volume. \[A\]
3. A clean object-oriented design, documented before coding.
4. Stability: no crashes, hangs or silent corruption on any input.

Non-goals (v1): GUI or rendering; interpolation other than nearest-neighbour; physical (millimetre) coordinates from NRRD `space directions`; floating-point voxel types.

### 3. Success metrics, assumptions and risks

- Success: all tests in SRS section 5 pass, the benchmark meets NFR-P1, and the robustness inputs never crash the program.
- Platform: Java 17 or later, standard library only. \[D\]
- Memory: a 1023³ volume needs about 1.07 GB (8-bit), 2.14 GB (16-bit) or 4.28 GB (32-bit), so start the JVM with enough `-Xmx`. \[D\]
- Risk: a plane with points far outside the volume could request a huge output. Mitigation: FR-G6.
- Risk: the MRI file used here has type short (signed 16-bit). Mitigation: the opt-in in FR-N4, with a check for negative values.
- Risk: JIT warm-up distorts FPS numbers. Mitigation: protocol in NFR-P1.

## Part II – Software Requirements (SRS)

### 1. Inputs and outputs

- **Volume M:** size `sx*sy*sz` with `1 <= sx,sy,sz < 2^10` (at most 1023); entries are unsigned integers of 8, 16 or 32 bits. \[A\]
- **Plane:** a triangle `(a,b,c)` or a trapezoid (`a'→b`, `a''→c`), with coordinates in voxel space. \[A\]\[D\]
- **Output:** image `E` of width `W` and height `H` (computed in 2.4), indexed `E[x][y]`; pixels with no valid source voxel are `0`. \[A\]

### 2. Functional requirements

#### 2.1 NRRD loading

- **FR-N1** Read the NRRD header (magic line, `key: value` fields, blank line) and the binary data. \[A\]
- **FR-N2** Supported: `dimension: 3`; `type` uint8/uchar, uint16/ushort, uint32/uint (and their long names); `encoding` raw or gzip; `endian` little or big. Detached data (`.nhdr`) is not supported. \[D\]
- **FR-N3** `sizes[0]` is `sx` and varies fastest in memory, then `sy`, then `sz`; linear index = `x + sx*(y + sy*z)`. \[D\]
- **FR-N4** Signed types (int8, int16, int32) are rejected with `NrrdFormatException` by default. With an explicit opt-in (allowSigned) they are accepted only if no voxel is negative (sign bit set); otherwise NrrdFormatException, so no value is silently misread. \[D\]
- **FR-N5** Any malformed, truncated or unsupported file, sizes outside `[1,1023]`, or data length different from `sx*sy*sz*bytesPerVoxel` raises `NrrdFormatException`. \[D\]

#### 2.2 Unsigned values

- **FR-U1** Read values as unsigned with `Byte.toUnsignedInt`, `Short.toUnsignedInt`, `Integer.toUnsignedLong` (or masks): stored `0xFF`, `0xFFFF`, `0xFFFFFFFF` read as 255, 65535, 4294967295. \[A\]
- **FR-U2** The image stores raw bits in an `int[]`; `get(x,y)` returns a `long` via `Integer.toUnsignedLong`. \[D\]

#### 2.3 The plane

- **FR-G1** Triangle: `Dab=dist(a,b)`; require `|Dab-dist(a,c)| <= REL_TOL*max(1,Dab)`. \[A\]\[D\]
- **FR-G2** Trapezoid: legs `a'→b` and `a''→c` have equal length `L` (same tolerance) and the four points are coplanar. Parallel legs (a parallelogram) are accepted. \[A\]\[D\]
- **FR-G3** An invalid plane throws `PlaneDefinitionException` at construction: NaN or Infinity coordinates, `L<EPS`, `Dbc<EPS`, collinear points, or failed checks above. \[D\]
- **FR-G4** A valid plane partly or fully outside the volume is not an error; outside pixels are `0`. \[A\]
- **FR-G5** For a trapezoid, `Dbc := max(dist(a',a''), dist(b,c))` (the longer base). \[D\]
- **FR-G6** `W` and `H` must not exceed `MAX_OUTPUT_DIM = 4096` (configurable), otherwise `PlaneDefinitionException`. \[D\]

#### 2.4 Projection algorithm

The assignment's algorithm is the reference; this is it stated precisely.

- **Normalise.** Triangle: `start1=start2=a`, `L=Dab`, `dir1=(b-a)/L`, `dir2=(c-a)/L` (unit vectors), `Dbc=dist(b,c)`. Trapezoid: `start1=a'`, `start2=a''`, `L=|a'b|`, `dir1=(b-a')/L`, `dir2=(c-a'')/L`, `Dbc` per FR-G5. \[A\]\[D\]
- **Output size.** `H = max(1, ceil(L))`, `W = max(1, ceil(Dbc))`; `E` starts all `0`. \[D\]

The row loop is written out step by step in Appendix A at the end of this document; the notes below explain its key points.

- Row `y` is sampled at distance `y+1` from the start, because the assignment steps `a1`, `a2` before sampling; kept for conformance. So the last row is at distance ceil(L), which can lie up to 1 unit beyond the far edge when L is not a whole number. It is still sampled, exactly as in the assignment: points outside the volume give 0 (FR-S2) and the guard keeps writes inside the image. \[A\]
- The `x1` loop is inclusive (`floor(d12)+1` samples per row) and the guard `0<=x<W` drops an extra edge sample. If `d12 < EPS` the row holds one sample and no division happens: this is the guard for `dp = vector(a1,a2)/d12`. \[A\]\[D\]
- `double` arithmetic over at most about 1800 steps keeps the accumulated error far below 1e-10.

#### 2.5 Sampling

- **FR-S1** Nearest neighbour with one rounding rule: `i = floor(px + 0.5)`, likewise `j`, `k`. Truncating with `(int)` is not allowed (it maps `-0.7` to `0`). \[A\]\[D\]
- **FR-S2** The bounds check uses the rounded integers (`0<=i<sx`, `0<=j<sy`, `0<=k<sz`), otherwise the result is `0`. For `sx=500`: `-0.5` inside, `-0.51` outside, `499.49` inside, `499.5` outside. \[A\]\[D\]

### 3. Non-functional requirements

- **NFR-P1** At least 30 FPS (must), 100+ FPS (expected) on a 500³ byte volume. \[A\] A frame is one `project()` call into a preallocated buffer, without display. Protocol \[D\]: 1000 warm-up frames discarded, 1000 measured, report median and worst 1% frame time, CPU, cores, JVM and thread count; planes tested: axis-aligned, 45° oblique, and one cutting along the slowest memory axis; output about `500*500`.
- **NFR-P2** No object allocation in the inner loops (verify: zero thread-allocated bytes over 1000 frames). Flat primitive arrays, loop invariants hoisted, no virtual call per voxel; a specialised loop per voxel type is allowed. \[A\]\[D\]
- **NFR-R1** For any accepted input `project()` never throws or hangs; invalid input fails fast with the documented exceptions at load or construction time. \[A\]\[D\]
- **NFR-R2** Deterministic output on every run. The largest linear index (1023³ - 1 = 1,070,599,166) fits an `int`, so single Java arrays suffice. \[D\]
- **NFR-M1** Immutable data types, single-responsibility classes, Javadoc on the public API, unit tests per class.

### 4. Design outline \[D\]

Names can vary; the structure is the point.

- `Volume` (interface: `sx()`, `sy()`, `sz()`, `long getUnsigned(x,y,z)`) with `ByteVolume`, `ShortVolume`, `IntVolume`, each holding one flat immutable array.
- `NrrdReader` parses a file into a `Volume` and throws `NrrdFormatException`.
- `PlaneDefinition` (sealed interface) with `TrianglePlane` and `TrapezoidPlane`; each validates in its constructor and exposes the normalised form (`start1`, `dir1`, `start2`, `dir2`, `L`, `Dbc`).
- `ProjectionImage` holds `W`, `H`, an `int[]` and `get(x,y)`; reusable, so frames allocate nothing.
- `Projector.project(Volume, PlaneDefinition, ProjectionImage)` implements 2.4; plain primitive fields (not objects) for vectors in the hot loop.

### 5. Acceptance tests

1. **Constant volume:** all voxels 255, plane inside; written pixels are 255, pixels the algorithm does not write stay 0.
2. **Unsigned values:** `0xFF`, `0xFFFF`, `0xFFFFFFFF` read as 255, 65535, 4294967295.
3. **Plane fully outside:** all pixels 0, no exception.
4. **Rounding boundaries** (`sx=500`): `-0.5`, `-0.51`, `499.49`, `499.5`, `-0.7` give inside, outside, inside, outside, outside.
5. **Brute-force equivalence:** on random volumes and valid planes (including axis-aligned slices of a volume that encodes its own coordinates) the optimised `Projector` equals a slow reference written straight from 2.4.
6. **Invalid planes:** collinear points, `b=c`, `a=b`, NaN, Infinity, non-isosceles, non-coplanar trapezoid, `ceil(L) > 4096` each throw `PlaneDefinitionException` at construction and nothing else.
7. **Tiny volumes and trapezoids:** `1*1*1` and `1*N*1` volumes; a rectangle and a true isosceles trapezoid give the expected width and centred rows; a leg length of 10.4 gives 11 rows, the last one sampled at distance 11, as in the assignment.
8. **NRRD:** raw and gzip, little and big endian, all three unsigned types load; truncated data, wrong `dimension`, unknown `type` or `encoding`, sizes `0` or `>=1024`, signed types without the option, and signed files with a negative voxel, each give the documented error; a signed file with only non-negative values loads when the option is on.
9. **Allocation and determinism:** zero bytes allocated over 1000 frames; single-threaded and multi-threaded runs give identical images.
10. **Performance:** NFR-P1 benchmark reports at least 30 FPS on all three planes, target 100+.

### 6. Decisions to confirm

The assignment is silent on these four points. Each has a recommended answer already used in this document.

1. **Signed voxel data (decided).** Your file has `type: short`, which means signed 16-bit numbers. The assignment says unsigned. Decision: use the opt-in of FR-N4. The loader checks every voxel. If none is negative, the values are read as unsigned and are exactly the same numbers. If a negative voxel exists, loading stops with an error instead of showing wrong values. Write this in the README.
2. **Rows beyond the far edge (decided).** Example: if `Dab` is 10.4, the loop makes 11 rows, and row 11 is sampled at distance 11, which is 0.6 outside the triangle. Decision: follow the assignment and sample that row too. Points outside the volume give `0`, and the check `0<=x<W` keeps all writes inside the image. The assignment also says that only points within the triangle are projected, but its loop samples this row too; we follow the loop. Write this in the README. \[D\]
3. **Trapezoid width.** In a trapezoid the two parallel edges have different lengths, for example 6 and 10. The image is a rectangle, so it must be at least 10 wide to fit the longer edge. Recommended: use the longer base (current). Alternative: use only `dist(b,c)`, which can cut off the longer edge when `dist(a',a'')` is bigger.
4. **Output cap.** `MAX_OUTPUT_DIM = 4096` only protects against asking for a huge image by mistake. Recommended: keep it; no need to ask anyone.

How to settle them: pick the recommended answer, write it in a Design Decisions section of your README, and send the instructor one short message about point 3 (what you chose and why) so a different expectation shows up before you code.

## Appendix A – Reference pseudo-code for 2.4

Pseudo-code, not Java. The slow reference implementation used in test 5 is written straight from it.

```
a1 = start1; a2 = start2
for y = 0 .. H-1:
    a1 += dir1; a2 += dir2          // as in the assignment: step first
    d12 = dist(a1, a2)
    dp  = (d12 < EPS) ? (0,0,0) : (a2 - a1) / d12
    x0  = round(W/2 - d12/2)        // [D] centres the row
    p   = a1
    for x1 = 0 .. floor(d12):       // assignment: x1 <= d12
        x = x0 + x1
        if (0 <= x < W) E[x][y] = sample(M, p)
        p += dp
return E
```
