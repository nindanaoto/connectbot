project(ConnectBot)

cmake_minimum_required (VERSION 3.4.1)

# Find Android log library
find_library (log-lib log)

# Local shell execution library (for Local transport)
add_library (com_google_ase_Exec SHARED "src/main/cpp/com_google_ase_Exec.cpp")
target_link_options (com_google_ase_Exec PRIVATE "-Wl,-z,max-page-size=16384")
target_link_libraries (com_google_ase_Exec ${log-lib})

# Mosh client JNI wrapper library
# This library provides the JNI interface for managing mosh-client processes.
# It handles forking, PTY creation, signal management, and process lifecycle
# for the native mosh-client subprocess.
add_library (moshclient SHARED "src/main/cpp/org_mosh_MoshClient.cpp")
target_link_options (moshclient PRIVATE "-Wl,-z,max-page-size=16384")
target_link_libraries (moshclient ${log-lib})

# NOTE: The full mosh native build requires additional dependencies:
# - zlib (compression)
# - protobuf (mosh protocol buffers)
# - ncurses (terminal handling and terminfo)
# - gmp (arbitrary precision math for crypto)
# - nettle (cryptographic functions)
# - mosh (the actual mosh-client binary)
#
# These are built as ExternalProjects when the libs/mosh submodule is present.
# For now, this CMakeLists.txt provides the JNI wrapper only.
# The actual mosh-client binary needs to be built separately or bundled.
#
# To enable full mosh build:
# 1. Add git submodule: git submodule add https://github.com/mobile-shell/mosh.git libs/mosh
# 2. Apply libs/mosh.patch for Android compatibility
# 3. Uncomment and configure the ExternalProject section below
#
# The terminfo.zip asset is generated during the build process when ncurses
# is compiled. It contains the terminfo database needed by mosh-client.

# Check if mosh submodule exists
set(MOSH_SRC_DIR "${CMAKE_SOURCE_DIR}/../libs/mosh")
if(EXISTS "${MOSH_SRC_DIR}/CMakeLists.txt" OR EXISTS "${MOSH_SRC_DIR}/configure.ac")
    message(STATUS "Mosh submodule found at ${MOSH_SRC_DIR}")
    # TODO: Add ExternalProject_Add commands for mosh and its dependencies
    # include(ExternalProject)
    # ExternalProject_Add(...)
else()
    message(STATUS "Mosh submodule not found. JNI wrapper will be built but")
    message(STATUS "native mosh-client binary needs to be provided separately.")
    message(STATUS "To add mosh support: git submodule add https://github.com/mobile-shell/mosh.git libs/mosh")
endif()
