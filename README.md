# Picto App *(Prototype)* Feature 

## Core Camera Features
- **Internal Storage Capture**
  - Photos are captured and saved directly to the app's private internal storage, keeping the main gallery clean.
- **Camera Switching** 
  - Button to toggle between front and back cameras.
- **Scrollable Capture Modes**
  - `ViewPager2` based carousel for selecting capture modes (Normal vs Filtered), snapping to the active button.
-  **Smart Gallery Thumbnail**
    - The gallery button dynamically updates to show a circular preview of the latest captured/imported image.

## Gallery & Management
-  **Private Gallery** 
    - A dedicated fragment displaying only images stored within the app's private directory.
-  **Manual Import**
    - Functionality to pick images from the device's system gallery and copy them into the app's private storage.
-  **Date-Based Organization**
    - Images are sorted and displayed based on modification date (newest first).

## Photo Viewing & Editing
-  **Swipeable Full-Screen Viewer**
    - `ViewPager2` implementation allowing users to swipe left/right to browse through their private gallery.
-  **Cropping**
    - Integrated `uCrop` library to provide robust image cropping functionality.
-  **Sharing**
    - Native Android share intent integration to share images to other apps.
-  **Deleting**
    - Ability to permanently remove images from the app's internal storage.
-  **Export/Save**
    - "Save" button to explicitly copy an image from the app's private storage to the device's public gallery.

## Filter System
-  **Scalable Filter Architecture**
    - A `FilterManager` object to easily register new filters with configuration (ID, button color, color matrix).
-  **Real-time Capture Filtering**
    - Captures are intercepted in memory, converted to Bitmaps, and processed with `ColorMatrix` filters before saving.
-  **Implemented Filters**:
    - *Normal*: Standard capture.
    - *Autumn*: A custom cinematic, warm filter using specific RGB scaling and saturation boosting.
    - *SumBlueThingy*: Don't mind the name, but it's kinda make your picture bit blue-ish looking

## UI/UX
-  **Custom Theming**
    - Application of specific color palette (Corn, Arlyde Yellow, etc.) across the app.# Picto App *(Prototype)* Feature 

## Core Camera Features
- **Internal Storage Capture**
  - Photos are captured and saved directly to the app's private internal storage, keeping the main gallery clean.
- **Camera Switching** 
  - Button to toggle between front and back cameras.
- **Scrollable Capture Modes**
  - `ViewPager2` based carousel for selecting capture modes (Normal vs Filtered), snapping to the active button.
-  **Smart Gallery Thumbnail**
    - The gallery button dynamically updates to show a circular preview of the latest captured/imported image.

## Gallery & Management
-  **Private Gallery** 
    - A dedicated fragment displaying only images stored within the app's private directory.
-  **Manual Import**
    - Functionality to pick images from the device's system gallery and copy them into the app's private storage.
-  **Date-Based Organization**
    - Images are sorted and displayed based on modification date (newest first).

## Photo Viewing & Editing
-  **Swipeable Full-Screen Viewer**
    - `ViewPager2` implementation allowing users to swipe left/right to browse through their private gallery.
-  **Cropping**
    - Integrated `uCrop` library to provide robust image cropping functionality.
-  **Sharing**
    - Native Android share intent integration to share images to other apps.
-  **Deleting**
    - Ability to permanently remove images from the app's internal storage.
-  **Export/Save**
    - "Save" button to explicitly copy an image from the app's private storage to the device's public gallery.

## Filter System
-  **Scalable Filter Architecture**
    - A `FilterManager` object to easily register new filters with configuration (ID, button color, color matrix).
-  **Real-time Capture Filtering**
    - Captures are intercepted in memory, converted to Bitmaps, and processed with `ColorMatrix` filters before saving.
-  **Implemented Filters**:
    - *Normal*: Standard capture.
    - *Autumn*: A custom cinematic, warm filter using specific RGB scaling and saturation boosting.
    - *SumBlueThingy*: Don't mind the name, but it's kinda make your picture bit blue-ish looking

## UI/UX
-  **Custom Theming**
    - Application of specific color palette (Corn, Arlyde Yellow, etc.) across the app. consistency
