🎬 NextScene: Take Your Watchlist to the Next Level!


NextScene is a modern Android mobile application that allows users to discover movies and TV series, view detailed information, and create and manage personalized watchlists (Favorites and Watched). The application combines the rich content of the OMDb API with the reliable account and data management features of Firebase.

✨ Key Features

🔍 Movie and Series Discovery: Easily search a vast movie and TV series database via the OMDb API.

🖼️ Detailed Insights: Access all critical information—poster, genre, runtime, release year, and IMDb rating—with a single tap.

🔐 Secure User Accounts:

    Firebase Authentication handles secure sign-up and login using email and password.

    Session Management ensures your status remains persistent.

📝 Personalized Watchlists (Powered by Firestore!):

    ❤️ Add/Remove from Favorites: Easily manage the content you love or plan to watch.

    ✅ Mark as Watched: Track completed viewings with a simple toggle.

    ⚡ Real-time Sync: All your lists are stored in user-specific sub-collections in Firebase Firestore and updated instantly across devices.

👤 Profile Management: A dedicated profile screen displaying the user's basic information (Username, Email) and a preview of their personalized movie/series lists.

📱 Modern and Fluid Interface: Developed with Material Design 3 principles, offering an intuitive and consistent user experience.

🛠️ Technologies Used

    Jetpack Compose
    Data Source	OMDb API	
    Retrofit	
    Kotlin Coroutines / Flow	
    Firebase Authentication
    Firebase Firestore

⚙️ Setup & Running

Follow these steps to set up and run the project in your local environment:

1. Clone the Repository

Bash

git clone https://github.com/emreata1/NextScene.git
cd NextScene

2. Firebase Setup

To run the project, you need to create a Firebase Project and connect it to the application.

    Create a new project in the Firebase console.

    Register your Android application with this project and place the google-services.json file in your project's app/ directory.

    Enable the Authentication (Email/Password) and Firestore Database services in your project.

3. Add OMDb API Key

You must obtain an OMDb API key and use it in your project.

    Add your API key to the local.properties file as follows:
    Properties

    OMDB_API_KEY="ENTER_YOUR_OMDB_API_KEY_HERE"

    (Note: The project must be configured to read the key via build.gradle or another configuration.)

4. Launch the Application

Open Android Studio and sync the project. You can then run the application on an emulator or a physical device.




<p align="center">
  <img src="https://github.com/emreata1/nextscene/blob/ee1abd4b8168ee199676dad9ed2c70f7ec0078e5/app/src/main/res/drawable/WhatsApp%20Image%202025-11-19%20at%2004.17.41%20(4).jpeg?raw=true" alt="Ekran Görüntüsü 4" width="15%" />
  
  <img src="https://github.com/emreata1/nextscene/blob/ee1abd4b8168ee199676dad9ed2c70f7ec0078e5/app/src/main/res/drawable/WhatsApp%20Image%202025-11-19%20at%2004.17.41%20(3).jpeg?raw=true" alt="Ekran Görüntüsü 3" width="15%" />
  
  <img src="https://github.com/emreata1/nextscene/blob/ee1abd4b8168ee199676dad9ed2c70f7ec0078e5/app/src/main/res/drawable/WhatsApp%20Image%202025-11-19%20at%2004.17.41%20(2).jpeg?raw=true" alt="Ekran Görüntüsü 2" width="15%" />
  
  <img src="https://github.com/emreata1/nextscene/blob/ee1abd4b8168ee199676dad9ed2c70f7ec0078e5/app/src/main/res/drawable/WhatsApp%20Image%202025-11-19%20at%2004.17.41%20(1).jpeg?raw=true" alt="Ekran Görüntüsü 1" width="15%" />
  
  <img src="https://github.com/emreata1/nextscene/blob/ee1abd4b8168ee199676dad9ed2c70f7ec0078e5/app/src/main/res/drawable/WhatsApp%20Image%202025-11-19%20at%2004.17.41%20(6).jpeg?raw=true" alt="Ekran Görüntüsü 6" width="15%" />
  
  <img src="https://raw.githubusercontent.com/emreata1/nextscene/ee1abd4b8168ee199676dad9ed2c70f7ec0078e5/app/src/main/res/drawable/WhatsApp%20Image%202025-11-19%20at%2004.17.41.jpeg?raw?=true" alt="Ekran Görüntüsü 5" width="15%" />
</p>
