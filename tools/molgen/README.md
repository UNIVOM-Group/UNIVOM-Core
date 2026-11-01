# molgen — 分子结构式 JSON 生成器

开发期离线工具，**不属于 UNIVOM Core 的构建**（不在根 `settings.gradle` 里），产物只有 JSON。
CDK 只在这里出现，游戏运行时不含 CDK 的任何一行代码。

## 用法

1. 编辑 `materials.tsv`，每行 `<材料ID><制表符或空格><SMILES>[<制表符>重复单元下标]`：

   ```
   gtceu:benzene	c1ccccc1
   gtceu:toluene	Cc1ccccc1
   gtceu:polyethylene	CC	n
   ```

   以 `#` 开头的行会被忽略。省略命名空间时默认 `gtceu`。第三列可选，写上之后会把
   所有原子套进方括号并加该下标，用于聚合物的重复单元。

2. 运行：

   ```bash
   ./gradlew run
   ```

3. 产物在 `out/assets/<命名空间>/molecules/<名字>.json`，把它整个复制到
   `src/main/resources/` 下即可。

生成器做了这些事：SMILES → CDK 芳香性感知 → Kekulé 化 → `StructureDiagramGenerator`
排布 2D 坐标 → 键长归一化为 1.0、整体居中 → 环上的双键自动翻转方向使副线画在环内侧、
非环双键标 `centered` → 芳环上的键标 `ring` / `aromatic` → 写出 JSON。碳原子按骨架式
惯例不画标签，杂原子上的隐式氢会根据邻接方向自动放到 `left` 或 `right`，带形式电荷的
原子会自动加上 `⁺` / `²⁻` 之类的上标。

## JSON 结构

文件放在 `assets/<命名空间>/molecules/<材料名>.json`，对应 GT 材料 `<命名空间>:<材料名>`。

```jsonc
{
  "contents": [ /* 下面几种节点的任意组合 */ ]
}
```

### atom

```jsonc
{
  "type": "atom",
  "index": 0,              // 唯一编号，bond/parens/circle 用它引用
  "element": "O",          // 省略则是不画标签的碳（骨架式顶点）
  "above": "H",            // 可选，画在主标签上方
  "right": ["H", 2],       // 可选，["符号", 个数] 会渲染成 H₂
  "below": "H",
  "left": "H",
  "x": 0.0, "y": 1.0       // 直角坐标；键长 1.0 为一个单位，y 向上
}
```

坐标也可以写成 `"u"` / `"v"`——两个从正 X 轴分别逆时针、顺时针偏 30° 的单位向量。
六元环骨架用 uv 写起来是整数，比 xy 好写：

```jsonc
{ "type": "atom", "index": 1, "u": 1.0, "v": 0.0 }
```

`element` 也可以写成对象，用来指定颜色或强制绑定某个 GT 材料：

```jsonc
{ "symbol": "Nq", "color": "#88FF88", "material": "gtceu:naquadah", "invisible": false }
```

**颜色一律取 GT 材料自身的 ARGB**：没写 `color` 时，先按 `material` 指定的材料取色；
没写 `material` 就按元素符号去查对应的 GT 材料（符号到材料的映射是启动时从 GT 自己的
材料注册表自动建的，不需要手写，符号后面的 `⁺` `²⁻` 等上标会被忽略）；实在查不到才
用配置里的 `defaultColor` 兜底。

### bond

```jsonc
{
  "type": "bond",
  "a": 0, "b": 1,
  "lines": ["solid", "solid"],   // 省略为单键
  "centered": true,              // 双键副线对称分布在轴两侧
  "ring": 0,                     // 可选，同一芳环上的键填同一个编号
  "aromatic": true               // 可选，标记这根键是环上「多出来的」那根双键
}
```

`ring` / `aromatic` 只影响配置项 `aromaticCircle`：打开后带 `aromatic` 的键会退化成单键，
并在环的质心处画一个圆（Thiele 式）。没有这两个字段的键永远按 `lines` 原样画。

`lines` 可用的值：

| 值 | 画法 |
|---|---|
| `solid` | 实线 |
| `dotted` | 虚线 |
| `thick` | 粗线 |
| `inward` | 向 b 收窄的虚楔形（指向纸内） |
| `outward` | 向 b 张开的实楔形（指向纸外） |

不写 `centered` 时，副线画在 a→b 方向的**逆时针一侧**。环上的双键要让副线落在环内，
把 `a` 和 `b` 对调即可（生成器会自动处理）。

### benzene

一次性展开成 6 个骨架顶点 + 交替单双键。`x0,y0` 是第一个顶点，`x1,y1` 是第二个，
其余按 ±60° 依次推出。顶点编号从 `start_index` 起连续 6 个。

```jsonc
{ "type": "benzene", "start_index": 0, "clockwise": false,
  "x0": 0.0, "y0": 0.0, "x1": 0.0, "y1": -1.0 }
```

同样支持 `u0,v0,u1,v1`。配置项 `aromaticCircle` 打开后，这类环会改画成单键 + 中心圆
（Thiele 式）而不是交替双键（Kekulé 式）。

### parens

方括号加上下标，用来标聚合物的重复单元或离子电荷。

```jsonc
{ "type": "parens", "sub": "n", "sup": "", "atoms": [1, 2] }
```

### circle

画一个椭圆，圆心自动取所列原子的质心。

```jsonc
{ "type": "circle", "x": 0.7, "y": 0.7, "atoms": [0, 1, 2, 3, 4, 5] }
```

也可以给 `a00` / `a01` / `a10` / `a11` 四个分量写任意 2×2 线性变换。

## 合金饼图

合金不需要逐个写文件。在 `assets/<命名空间>/chemistry/alloys.json` 里列出要画饼图的
材料 ID 即可，成分由 GT 的 `Material.getMaterialComponents()` 自动递归推导：

```jsonc
{ "materials": ["gtceu:steel", "gtceu:stainless_steel"] }
```

同名文件在多个命名空间下都会被读取并合并，资源包和附属模组可以各自补充。
