<p align="center">
  <img src="https://i.imgur.com/QeCE3gg.png" alt="Navigate to Items Page" width="800"/>
</p>
<h1>Closets: Your Personal Wardrobe Manager</h1>
<p>
  <em>Organize, track, and style your wardrobe with ease.</em>
</p>

[Download Closets APK](https://drive.google.com/file/d/1yh_3t6iouT9vukH8dTsPEBFp7dRrnvK2/view?usp=sharing)

<hr/>

<h2 id="-tech-stack">🛠️ Tech Stack</h2>
<ul>
  <li><strong>Language:</strong> Kotlin</li>
  <li><strong>Architecture:</strong> MVVM</li>
  <li><strong>Local DB:</strong> Room</li>
  <li><strong>UI:</strong> AndroidX (ConstraintLayout, Material), ViewBinding</li>
  <li><strong>Image Loading:</strong> Glide</li>
  <li><strong>Networking:</strong> Retrofit + Gson</li>
  <li><strong>Background Work:</strong> WorkManager</li>
  <li><strong>Notifications:</strong> WorkManager + Android Notifications</li>
  <li><strong>Performance:</strong> Firebase Performance Monitoring</li>
  <li><strong>Export/Import:</strong> ZIP via <code>ZipOutputStream</code> / <code>ZipInputStream</code></li>
</ul>

<hr/>

<h2 id="-prerequisites">📋 Prerequisites</h2>
<ul>
  <li>Android Studio Flamingo or later</li>
  <li>JDK 11+</li>
  <li>Android device/emulator API ≥ 24</li>
</ul>

<hr/>

<h2 id="-installation--setup">🚀 Installation &amp; Setup</h2>
<ol>
  <li><strong>Clone the repo</strong><br/>
    <pre><code>git clone https://github.com/2hulie/closets.git
cd closets</code></pre>
  </li>
  <li><strong>Checkout your branch</strong><br/>
    <pre><code>git checkout release-v1</code></pre>
  </li>
  <li><strong>Open in Android Studio</strong><br/>
    <em>File &gt; Open</em> → select project root. Let Gradle sync.</li>
  <li><strong>Run the app</strong><br/>
  </li>
</ol>

<hr/>

<h2 id="-usage">📱 Usage</h2>

<h3>1. Outfit of the Day</h3>
<ol>
  <li>Tap the <strong>hanger</strong> icon on Home.</li>
  <li>Select <em>View Today’s Outfit</em>.</li>
  <li>Tap <strong>✎</strong> to edit, pick items, <strong>Save</strong>, then <strong>Save Outfit</strong>.</li>
</ol>

<h3>2. Wardrobe Management</h3>
<ul>
  <li><strong>Add Item:</strong> Wardrobe tab → <strong>+</strong> → fill details → <strong>Add Item to Closet</strong></li>
  <li><strong>Edit Item:</strong> Tap item → <strong>Edit Item Info</strong> → modify → <strong>Save Changes</strong></li>
  <li><strong>Delete Item(s):</strong>
    <ul>
      <li>Single: Item details → <strong>Delete</strong> → confirm</li>
      <li>Multiple: Wardrobe → ⋮ → <strong>Select Multiple</strong> → pick → <strong>Delete Items</strong></li>
    </ul>
  </li>
</ul>

<h3>3. Search &amp; Filter</h3>
<ul>
  <li>Tap <strong>🔍</strong> to search by name.</li>
  <li>Tap <strong>≡</strong> filter icon → choose type/color → <strong>Apply</strong> or <strong>Reset</strong>.</li>
</ul>

<h3>4. Export / Import Data</h3>
<ul>
  <li><strong>Export:</strong> Data tab → <strong>Export</strong> → save ZIP.</li>
  <li><strong>Import:</strong> Data tab → <strong>Import</strong> → <em>Update</em> or <em>Replace</em> → select ZIP.</li>
</ul>

<hr/>

<h2 id="-screenshots">📸 How to Add a Clothing Item</h2>
<table>
  <tr>
    <th>1. Navigate to Items Page</th>
    <th>2. Fill in the Fields</th>
    <th>3. Tap Add Item to Closet</th>
  </tr>
  <tr>
    <td>
      <img src="https://i.imgur.com/oxmYQ5O.png" alt="Navigate to Items Page" width="200"/>
    </td>
    <td>
      <img src="https://i.imgur.com/VP0fSxl.png" alt="Fill in the Fields" width="200"/>
    </td>
    <td>
      <img src="https://i.imgur.com/Eo90hQS.png" alt="Tap Add Item to Closet" width="200"/>
    </td>
  </tr>
</table>

---

<h2 id="-acknowledgements">🙏 Acknowledgements</h2>
<ul>
  <li><a href="https://developer.android.com/jetpack/androidx">AndroidX</a></li>
  <li><a href="https://github.com/bumptech/glide">Glide</a></li>
  <li><a href="https://square.github.io/retrofit/">Retrofit &amp; Gson</a></li>
  <li><a href="https://developer.android.com/jetpack/androidx/releases/room">Room</a></li>
  <li><a href="https://firebase.google.com/docs/perf-mon">Firebase Performance</a></li>
  <li><a href="https://github.com/CanHub/Android-Image-Cropper">CanHub Image Cropper</a></li>
</ul>
