# Branding master

`ic_app_master.png` is the 2048x2048 render of the app mark, and the source every shipped copy is derived from.
It is a 3D render rather than line art, so there is no vector version and none can be made from it.

It lives here rather than in a `res/` directory because nothing should ship it.
Everything the app builds against is generated from it at the size that is actually drawn:

| generated | from | size |
| --- | --- | --- |
| `ui-base/src/main/res/drawable-*/ic_app.webp` | resized | 74dp - the `ImageView` on the backup screens |
| `app/src/main/res/drawable-*/ic_splash_icon.webp` | resized, centred on a 288dp canvas inside the 192dp circle | the API 31+ system splash |

`fastlane/metadata` is what `supply` uploads, so this sits outside it deliberately.

The reason this file is kept at all: the same artwork once shipped at 2048x2048 in five density buckets, 3.7 MB of an APK, to fill a 74dp view.
Regenerating from a master beats resampling whatever copy is nearest to hand.
