group = "app.focusguard"

patches {
    about {
        name = "Focus Guard Patches"
        description = "Small, targeted patches that add friction to disabling selected focus tools."
        source = "https://github.com/your-name/focus-guard-patches"
        author = "Focus Guard contributors"
        contact = "Open an issue in the repository"
        website = "https://github.com/dusk-beep/focus-guard-patches"
        license = "GPLv3"
    }
}

dependencies {
    compileOnly(libs.gson)
}
