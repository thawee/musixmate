./gradlew clean
./gradlew assembleDebug
cp mobile/build/outputs/apk/mobile-debug.apk dist/musixmate.apk
git add dist/musixmate.apk
git commit -m "New Build"
git push origin master
