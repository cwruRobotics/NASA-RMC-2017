# This script builds the client. The resulting binaries are stored in ./client/build
# To start the client, execute ./client/build/GlenniferClient.exe

# Build protobuf classes

.\build-pb.ps1

# Build client

cd .\client\
New-Item -Force -ItemType directory -Path build
cd .\build\
qmake -r ..\GlenniferClient.pro CONFIG+=debug
nmake
cd ..
cd ..