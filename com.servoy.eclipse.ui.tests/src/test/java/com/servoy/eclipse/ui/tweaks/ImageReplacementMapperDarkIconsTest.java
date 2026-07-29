package com.servoy.eclipse.ui.tweaks;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SVY-21149: Verifies that dark-theme icon files referenced by ImageReplacementMapper
 * URL mappings actually exist in the com.servoy.eclipse.ui.tweaks project.
 *
 * The bug was caused by a missing expandall-disabled.png in darkicons/, which led to
 * FileNotFoundException and cascading DecorationOverlayIcon errors during Ctrl+H search.
 */
@DisplayName("SVY-21149: Dark theme icon files exist in ui.tweaks bundle")
class ImageReplacementMapperDarkIconsTest
{

	private static final Path UI_TWEAKS_ROOT;

	static
	{
		Path startDir = Paths.get("").toAbsolutePath();
		Path candidate = startDir.resolve("../com.servoy.eclipse.ui.tweaks").normalize();
		if (!Files.isDirectory(candidate))
		{
			candidate = startDir.getParent().resolve("com.servoy.eclipse.ui.tweaks").normalize();
		}
		UI_TWEAKS_ROOT = candidate;
	}

	@Nested
	@DisplayName("expandall-disabled icons in darkicons/")
	class ExpandAllDisabledIcons
	{

		@Test
		@DisplayName("darkicons/expandall-disabled.png exists and is a valid 16x16 PNG")
		void expandAllDisabled1xExists() throws Exception
		{
			assertTrue(Files.isDirectory(UI_TWEAKS_ROOT), "com.servoy.eclipse.ui.tweaks project must be reachable");
			Path iconPath = UI_TWEAKS_ROOT.resolve("darkicons/expandall-disabled.png");
			assertTrue(Files.exists(iconPath), "darkicons/expandall-disabled.png must exist");

			BufferedImage img = ImageIO.read(iconPath.toFile());
			assertNotNull(img, "darkicons/expandall-disabled.png must be a valid image");
			assertAll(
				() -> assertEquals(16, img.getWidth(), "width should be 16px"),
				() -> assertEquals(16, img.getHeight(), "height should be 16px"));
		}

		@Test
		@DisplayName("darkicons/expandall-disabled@2x.png exists and is a valid 32x32 PNG")
		void expandAllDisabled2xExists() throws Exception
		{
			assertTrue(Files.isDirectory(UI_TWEAKS_ROOT), "com.servoy.eclipse.ui.tweaks project must be reachable");
			Path iconPath = UI_TWEAKS_ROOT.resolve("darkicons/expandall-disabled@2x.png");
			assertTrue(Files.exists(iconPath), "darkicons/expandall-disabled@2x.png must exist");

			BufferedImage img = ImageIO.read(iconPath.toFile());
			assertNotNull(img, "darkicons/expandall-disabled@2x.png must be a valid image");
			assertAll(
				() -> assertEquals(32, img.getWidth(), "width should be 32px"),
				() -> assertEquals(32, img.getHeight(), "height should be 32px"));
		}
	}

	@Nested
	@DisplayName("parity between icons/ and darkicons/ for expandall variants")
	class IconParityCheck
	{

		@Test
		@DisplayName("all expandall icon variants exist in both icons/ and darkicons/")
		void allExpandAllVariantsExistInBothPaths()
		{
			assertTrue(Files.isDirectory(UI_TWEAKS_ROOT), "com.servoy.eclipse.ui.tweaks project must be reachable");
			String[] filenames = {
				"expandall.png",
				"expandall@2x.png",
				"expandall-disabled.png",
				"expandall-disabled@2x.png"
			};

			assertAll(
				() ->
				{
					for (String filename : filenames)
					{
						assertTrue(Files.exists(UI_TWEAKS_ROOT.resolve("icons/" + filename)),
							"icons/" + filename + " must exist");
					}
				},
				() ->
				{
					for (String filename : filenames)
					{
						assertTrue(Files.exists(UI_TWEAKS_ROOT.resolve("darkicons/" + filename)),
							"darkicons/" + filename + " must exist");
					}
				});
		}

		@Test
		@DisplayName("collapseall disabled icons exist in both directories (regression guard)")
		void collapseAllDisabledIconsExist()
		{
			assertTrue(Files.isDirectory(UI_TWEAKS_ROOT), "com.servoy.eclipse.ui.tweaks project must be reachable");
			String[] filenames = {
				"collapseall-disabled.png",
				"collapseall-disabled@2x.png"
			};

			assertAll(
				() ->
				{
					for (String filename : filenames)
					{
						assertTrue(Files.exists(UI_TWEAKS_ROOT.resolve("icons/" + filename)),
							"icons/" + filename + " must exist");
					}
				},
				() ->
				{
					for (String filename : filenames)
					{
						assertTrue(Files.exists(UI_TWEAKS_ROOT.resolve("darkicons/" + filename)),
							"darkicons/" + filename + " must exist");
					}
				});
		}
	}
}
