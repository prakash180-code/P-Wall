# P-Wall Premium Development Roadmap

## Overview

The core application is already complete.

The remaining work should be completed in small, controlled phases.
Never ask the coding agent to implement multiple major features at once.

## Recommended Workflow

``` text
Core App (Completed)
        │
        ▼
Prompt 1 – Architecture Refactor
        │
Compile & Test
        │
Git Commit
        ▼
Prompt 2 – Premium 3D Parallax
        │
Compile & Test
        │
Git Commit
        ▼
Prompt 3 – AI Depth Engine
        │
Compile & Test
        │
Git Commit
        ▼
Prompt 4 – Manual Depth Editor
        │
Compile & Test
        │
Git Commit
        ▼
Prompt 5 – Premium UI & Effects
        │
Compile & Test
        │
Git Commit
        ▼
Prompt 6 – Optimization & Release
        │
Release v1.0
        ▼
Prompt 7 – Supabase Licensing
        ▼
Prompt 8 – Admin Portal
```

------------------------------------------------------------------------

# Rules

## Before every prompt

``` bash
git add .
git commit -m "Before Prompt X"
```

## After every prompt

Ask the coding agent to:

-   Compile the project.
-   Fix every compilation error.
-   Run static analysis.
-   Check for memory leaks.
-   Verify that no existing feature is broken.
-   Stop and wait for approval.

## After successful completion

``` bash
git add .
git commit -m "Completed Prompt X"
```

Never continue automatically to the next phase.

------------------------------------------------------------------------

# Prompt 1 -- Architecture Refactor

Goal: Prepare the project for future premium features without changing
visible functionality.

Implement:

-   Wallpaper Render Engine
-   Layer System
-   Module System
-   Effect Manager
-   Background Layer
-   Clock Layer
-   Date Layer
-   Foreground Layer
-   Overlay Layer

Requirements:

-   No UI changes
-   No feature changes
-   Same behaviour
-   Future features must be modular

Wait for approval.

------------------------------------------------------------------------

# Prompt 2 -- Premium 3D Engine

Implement:

-   Accelerometer support
-   Gyroscope support
-   Smooth interpolation
-   Background movement
-   Foreground movement
-   Sensitivity setting
-   Strength setting
-   Motion smoothing
-   Battery optimization
-   Disable automatically on unsupported devices

Movement should feel subtle and premium.

Wait for approval.

------------------------------------------------------------------------

# Prompt 3 -- AI Depth Engine

Implement:

-   Google ML Kit segmentation
-   Offline processing
-   Foreground extraction
-   Background extraction
-   Cached masks
-   Clock behind foreground object
-   Automatic fallback
-   Automatic caching

Requirements:

-   Segmentation only when wallpaper changes.
-   Never segment every frame.

Wait for approval.

------------------------------------------------------------------------

# Prompt 4 -- Manual Depth Editor

Implement:

-   Preview editor
-   Expand mask
-   Shrink mask
-   Feather edges
-   Smooth edges
-   Reset mask
-   Save edited mask

Prepare architecture for future:

-   Brush
-   Eraser
-   Polygon selection

Do not implement those future tools yet.

Wait for approval.

------------------------------------------------------------------------

# Prompt 5 -- Premium UI & Effects

Implement:

Glass Clock

-   Blur
-   Glow
-   Border
-   Opacity

Dynamic Colors

-   Extract dominant wallpaper colors
-   Automatic clock colors
-   Manual override

Micro Animations

-   Fade transitions
-   Smooth seconds
-   Gentle breathing animation

Wallpaper

-   Cinematic zoom

Premium Settings

Everything configurable.

Wait for approval.

------------------------------------------------------------------------

# Prompt 6 -- Production Optimization

Implement:

-   Bitmap cache
-   Paint cache
-   Render optimization
-   Memory optimization
-   Battery optimization
-   Low-end device mode
-   Error handling
-   Logging
-   Documentation

Prepare architecture for future:

-   Weather
-   Particles
-   Music controls
-   Battery widget
-   Calendar
-   GIF wallpaper
-   Video wallpaper

Do not implement those features.

Generate Release APK.

Update:

-   README
-   CHANGELOG
-   TODO

Wait for approval.

------------------------------------------------------------------------

# Prompt 7 -- Supabase Licensing (Future)

Implement:

-   Device registration
-   Activation
-   Online License Manager
-   Offline cache
-   Device binding
-   Force update
-   Activation screen
-   Admin API

------------------------------------------------------------------------

# Prompt 8 -- Admin Portal (Future)

Create web dashboard.

Features:

-   User management
-   Activate
-   Deactivate
-   Expiry
-   Device management
-   Version management
-   Statistics

------------------------------------------------------------------------

# Review Checklist After Every Prompt

1.  Did you change any existing architecture?
2.  Did you remove any feature?
3.  Which files changed?
4.  Which new files were created?
5.  Any TODOs?
6.  Any performance concerns?
7.  Any memory leaks?
8.  Does the project compile?
9.  Is the APK generated successfully?
10. Can we safely continue?

------------------------------------------------------------------------

# Suggested Git Commit Names

-   Initial Stable
-   Architecture Refactor
-   Premium 3D Engine
-   AI Depth Engine
-   Manual Depth Editor
-   Premium UI
-   Production Optimization
-   Version 1.0
-   Supabase Licensing
-   P-License Portal

------------------------------------------------------------------------

# Final Roadmap

## Version 1.0

-   Custom Wallpaper
-   Live Clock
-   Live Date
-   Premium 3D Parallax
-   AI Depth Effect
-   Clock Behind Subject
-   Glass UI
-   Dynamic Colors
-   Cinematic Wallpaper
-   Manual Subject Correction
-   Production Quality

## Version 1.1

-   Supabase Licensing

## Version 1.2

-   Admin Portal

## Version 2.0

-   Weather
-   Particles
-   Video Wallpapers
-   GIF Wallpapers
-   Widgets
-   Theme Store

------------------------------------------------------------------------

# Best Practice

Instead of asking the coding agent to paste large amounts of code in
chat, instruct it to modify the project files directly and then provide
only:

1.  Summary of changes
2.  Files modified
3.  Files added
4.  Manual steps required
