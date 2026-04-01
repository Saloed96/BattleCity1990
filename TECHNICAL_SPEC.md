# Battle City 1990 - Technical Specification

## Project Overview
- **Package**: `com.saloed.bcity`
- **Engine**: JavaFX 23.0.1
- **Java Version**: 26
- **Build Tool**: Gradle 9.4+
- **Main Class**: `com.saloed.bcity.Main`

## Display & Resolution
- **Game Width**: 1600 pixels (50 tiles × 32px)
- **Game Height**: 1184 pixels (37 tiles × 32px)
- **Tile Base Size**: 16×16 pixels
- **Tile Render Scale**: 2× (32×32 pixels final)
- **FPS Target**: 60 FPS
- **Update Rate**: 60 updates/second

## Texture Atlas (texture_atlas.png)
- **Dimensions**: 400×256 pixels
- **Grid**: 25 tiles wide × 16 tiles high (each tile 16×16px)
- **Coordinate System**: 0-based pixel coordinates

### Tile Coordinates (1-based counting → pixel coords)

#### Terrain Tiles (16×16 each):
| Tile Type | Tile # | Row | Pixel X | Pixel Y | Notes |
|-----------|--------|-----|---------|---------|-------|
| BRICK     | 17     | 1   | 256     | 0       | Destructible wall |
| STEEL     | 17     | 2   | 256     | 16      | Indestructible |
| WATER     | 17,17,18 | 3,4 | 256,256,272 | 32,48,48 | 3-frame animation |
| GRASS     | 18     | 3   | 272     | 32      | Overlays tanks |
| ICE       | 19     | 3   | 288     | 32      | Slippery surface |
| EAGLE     | 20     | 3   | 304     | 32      | Base headquarters |
| EAGLE_DEAD| 21     | 3   | 320     | 32      | Destroyed base |

#### Player Tank (16×16 per frame, 2 frames per direction):
| Direction | Tiles | Row | Pixel X Range | Pixel Y | Frames |
|-----------|-------|-----|---------------|---------|--------|
| NORTH     | 1-2   | 1   | 0-31          | 0       | 2      |
| WEST      | 3-4   | 1   | 32-63         | 0       | 2      |
| SOUTH     | 5-6   | 1   | 64-95         | 0       | 2      |
| EAST      | 7-8   | 1   | 96-127        | 0       | 2      |

#### Enemy Tanks (16×16 per frame, 2 frames per direction):
| Type  | Row | Pixel Y | Directions (X ranges) |
|-------|-----|---------|----------------------|
| BASIC | 5   | 64      | N:128-159, W:160-191, S:192-223, E:224-255 |
| FAST  | 6   | 80      | Same X as BASIC |
| POWER | 7   | 96      | Same X as BASIC |
| ARMOR | 8   | 112     | Same X as BASIC |

#### Bullets (variable sizes, Row 7):
| Direction | Tile | Quadrant | Pixel X | Pixel Y | Width | Height |
|-----------|------|----------|---------|---------|-------|--------|
| UP        | 21   | bottom-left  | 323     | 102     | 3     | 4      |
| LEFT      | 21   | bottom-right | 330     | 102     | 4     | 3      |
| DOWN      | 22   | bottom-left  | 339     | 102     | 3     | 4      |
| RIGHT     | 22   | bottom-right | 346     | 102     | 4     | 3      |

**Scale**: 2× (final rendered size: 6×8 or 8×6 pixels)

#### Explosions:

**SMALL (bullet hit)** - 3 frames, 16×16 each:
| Frame | Tile | Row | Pixel X | Pixel Y |
|-------|------|-----|---------|---------|
| 1     | 17   | 9   | 256     | 128     |
| 2     | 18   | 9   | 272     | 128     |
| 3     | 19   | 9   | 288     | 128     |

**BIG (tank destroyed)** - 6 frames:
| Frame | Source | Size | Pixel X | Pixel Y | Dimensions |
|-------|--------|------|---------|---------|------------|
| 1-3   | Tiles 17-19, Row 9 | 16×16 | 256-288 | 128 | Same as SMALL |
| 4     | Tile 20, Row 9 | 32×32 | 304 | 128 | Large explosion |
| 5     | Tile 22, Row 9 | 32×32 | 336 | 128 | Large explosion |
| 6     | Tile 19, Row 9 | 16×16 | 288 | 128 | Final frame |

**SPAWN (tank appearance)** - 13 frames, 16×16 each:
Sequence: [4,3,2,1,2,3,4,3,2,1,2,3,4] where 1=Tile17, 2=Tile18, 3=Tile19, 4=Tile20
All from Row 7 (Pixel Y = 96), X ranges: 256-304

## Animation Details

### Water Animation
- **Frames**: 3 (Tiles 17,17,18 from Row 3 and 4)
- **Frame 1**: Tile 17, Row 3 → (256, 32)
- **Frame 2**: Tile 17, Row 4 → (256, 48)
- **Frame 3**: Tile 18, Row 4 → (272, 48)
- **Speed**: Updates every 6 frames (~10 FPS at 60 FPS game speed)
- **Implementation**: Custom L-shaped sprite extraction in Tile.createSprite()

### Player Tank Animation
- **Frames per direction**: 2
- **Animation speed**: Changes every 8 frames while moving
- **Trigger**: Only animates when tank is moving
- **Code**: `animationFrame++` then `setSpriteIndex((animationFrame / 8) % 2)`

### Enemy Tank Animation
- Same as player: 2 frames per direction, updates every 8 frames

### Spawn Animation
- **Total frames**: 13
- **Sequence**: 4→3→2→1→2→3→4→3→2→1→2→3→4
- **Speed**: 6 frames per sprite = 78 total frames (~1.3 seconds at 60 FPS)
- **Tank visibility**: Tank becomes visible ONLY after frame 12 (last frame)
- **Critical**: `spawnCallbackTriggered` flag ensures one-time trigger

## Entity Sizes & Collision

### Default Entity Dimensions:
- **Player**: 32×32 pixels (16×16 base × 2 scale)
- **Enemy**: 32×32 pixels (same as player)
- **Bullet**: 
  - UP/DOWN: 6×8 pixels (3×4 base × 2 scale)
  - LEFT/RIGHT: 8×6 pixels (4×3 base × 2 scale)

### Collision Boxes:
```java
// Entity.intersects() uses AABB collision
boolean intersects(Entity other) {
    return x < other.x + otherWidth &&
           x + thisWidth > other.x &&
           y < other.y + otherHeight &&
           y + thisHeight > other.y;
}
```

## Game Mechanics

### Spawning System:
1. **Player respawn**: Position (800, 1100), triggered after death
2. **Enemy spawn**: Positions [32, 384, 736] at Y=32, random selection
3. **Spawn process**:
   - Create tank with `alive=false`, `spawning=true`
   - Create SPAWN explosion with reference to tank entity
   - Play 13-frame animation
   - On frame 12: call `finishSpawning()` → `alive=true`, `spawning=false`
   - Tank becomes visible and vulnerable

### Invulnerability:
- Player is invulnerable during spawn animation
- Becomes vulnerable when `finishSpawning()` is called
- Enemy bullets check `!player.isInvulnerable()` before hit

### Bullet Collisions:
- Player bullets destroy enemy bullets (mutual destruction)
- Creates SMALL explosion at collision point
- Checked in Game.update() bullet iteration loop

### Tank Collisions:
- Player ↔ Enemies: Blocking collision
- Enemy ↔ Enemy: Blocking collision
- Implemented in Game.update() with position rollback

## Critical Implementation Details

### Explosion Class Structure:
```java
public class Explosion extends Entity {
    private Image[] frames;  // Pre-extracted sprite frames
    private Entity spawningEntity;  // Reference to spawning tank
    private boolean spawnCallbackTriggered;  // One-shot flag
    
    // In update():
    if (type == SPAWN && spawningEntity != null && 
        !spawnCallbackTriggered && currentFrame >= maxFrames - 1) {
        spawnCallbackTriggered = true;
        if (spawningEntity instanceof Player) { ... }
        else if (spawningEntity instanceof Enemy) { ... }
    }
}
```

### Sprite Rendering Chain:
```
TextureAtlas.cut(x, y, w, h)
  → ResourceLoader.cutImage(source, x, y, w, h)
    → Creates WritableImage
    → Handles transparency (black pixels → transparent)
    
SpriteSheet.getSprite(index)
  → Calculates position from index
  → Returns Image
  
Sprite.render(g, x, y)
  → Calls sheet.getSprite(currentIndex)
  → Draws with scale: g.drawImage(img, x, y, w*scale, h*scale)
```

### Water Animation Special Case:
- Cannot use horizontal sprite strip (L-shaped layout)
- Extracts 3 individual frames separately
- Combines into single 48×16 WritableImage using Canvas
- Creates SpriteSheet from combined image

## File Structure
```
src/main/
├── java/com/saloed/bcity/
│   ├── display/Display.java          # JavaFX window, buffers
│   ├── game/
│   │   ├── Bullet.java               # Projectile logic
│   │   ├── Enemy.java                # AI tanks
│   │   ├── Entity.java               # Base class with collision
│   │   ├── EntityType.java           # Enum: Player, Enemy, Bullet
│   │   ├── Explosion.java            # Animation system
│   │   ├── Game.java                 # Main game loop
│   │   ├── Level.java                # Tile map management
│   │   ├── Player.java               # Player control
│   │   ├── Tile.java                 # Individual tile
│   │   └── TileType.java             # Enum with properties
│   ├── graphics/
│   │   ├── Sprite.java               # Renderable sprite
│   │   ├── SpriteSheet.java          # Frame extraction
│   │   └── TextureAtlas.java         # Atlas wrapper
│   ├── io/Input.java                 # Keyboard state
│   ├── utils/
│   │   ├── ResourceLoader.java       # Image loading/cutting
│   │   └── Time.java                 # Time utilities
│   └── Main.java                     # Application entry
└── resources/
    └── texture_atlas.png             # 400x256 sprite sheet
```

## Known Issues & Debugging Tips

### Spawn System:
- **Problem**: Tanks not appearing after animation
- **Check**: Console output "Player/Enemy tank spawned at X,Y"
- **Debug points**:
  1. Verify `spawningEntity` is not null in Explosion constructor
  2. Check `currentFrame` reaches 12 (for 13-frame animation)
  3. Ensure `spawnCallbackTriggered` starts false
  4. Confirm `finishSpawning()` sets `alive=true`

### Coordinate Calculation:
```java
// 1-based tile counting to 0-based pixels:
int pixelX = (tileNumber - 1) * 16;
int pixelY = (rowNumber - 1) * 16;

// Example: Tile 17, Row 9
pixelX = (17 - 1) * 16 = 256
pixelY = (9 - 1) * 16 = 128
```

### Transparency Handling:
- `ResourceLoader.cutImage()` converts near-black pixels to transparent
- Threshold: RGB < 30 and Alpha > 128 → fully transparent
- Preserves alpha channel from PNG

## Performance Considerations
- **Buffer Strategy**: 3 buffers for smooth rendering
- **Animation Updates**: Only when needed (e.g., movement for tanks)
- **Bullet Cleanup**: Immediate removal on deactivation
- **Explosion Frames**: Pre-extracted Image array (no runtime cutting)

## Build & Run
```bash
gradle build
gradle run
```

Or run `Main.java` directly from IDE.
