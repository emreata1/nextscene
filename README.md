🎬 NextScene

🌟 Project Name and Brief Description

NextScene is a modern Android mobile application that allows users to discover movies and TV series, view detailed information, and create and manage personalized watchlists (Favorites and Watched). The application combines the rich content of the OMDb API with the reliable account and data management features of Firebase.

✨ Key Features

    🔍 Movie and Series Discovery: Easily search a vast movie and TV series database via the OMDb API.

    🖼️ Detailed Content Information: Display of detailed information for each selected content, such as poster, synopsis (plot), genre, runtime, release year, and IMDb rating.

    🔐 Secure User Account Management:

        Sign-Up and Login: Secure user account creation and sign-in with email and password (Firebase Authentication).

        Session Management: Persistent management of the user's session status.

    📝 Personalized Watchlists (Firebase Firestore):

        Add/Remove to Favorites: Users can add and remove movies and series from their favorite lists.

        Mark as Watched: Feature to mark content as "Watched" for tracking completed viewings.

        Real-time Synchronization: All lists are stored in user-specific sub-collections in Firestore and updated instantly across devices.

    👤 Profile Management: A dedicated profile screen displaying the user's basic information (Username, Email) and a preview of their personalized movie/series lists.

    📱 Modern and Fluid Interface: Developed with Material Design 3 principles, offering an intuitive and consistent user experience.

🛠️ Technologies Used

This project was developed using the following main technologies and libraries:
Area	Technology/Library	Description
Development Language	Kotlin	Preferred programming language for modern and safe Android applications.
UI Development	Jetpack Compose	Modern, flexible, and fast UI development with a declarative approach.
Data Source	OMDb API	External REST API providing movie and series data.
API Communication	Retrofit	Type-safe HTTP client and management of API calls.
Asynchronous Ops	Kotlin Coroutines / Flow	Management of non-blocking operations and asynchronous data streams.
Authentication	Firebase Authentication	User sign-up, login, and session management.
Database	Firebase Firestore	Real-time, NoSQL cloud database for list synchronization.
Navigation	Jetpack Compose Navigation	Managing transitions between screens.
Image Loading	Coil	A fast and lightweight image loading library for Android.

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

🤝 Contributing

Feel free to contribute with feedback, bug reports, and new features!

    Fork the Project

    Create your Feature Branch (git checkout -b feature/AmazingFeature)

    Commit your Changes (git commit -m 'Add AmazingFeature')

    Push to the Branch (git push origin feature/AmazingFeature)

    Open a Pull Request

📄 License

This project is licensed under the MIT License. See the LICENSE file for more details.
