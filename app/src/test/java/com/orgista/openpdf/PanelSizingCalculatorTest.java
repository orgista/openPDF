package com.orgista.openpdf;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class PanelSizingCalculatorTest {
    private final PanelSizingCalculator calculator = new PanelSizingCalculator();

    @Test
    public void clampsProgressIntoExpectedRange() {
        PanelSizingCalculator.Size size = calculator.calculate(1000, 700, -20, 140);

        assertEquals(350, size.width);
        assertEquals(672, size.height);
    }

    @Test
    public void enforcesMinimumPanelDimensions() {
        PanelSizingCalculator.Size size = calculator.calculate(100, 100, 0, 0);

        assertEquals(320, size.width);
        assertEquals(220, size.height);
    }

    @Test
    public void scalesWithinTheAvailableStage() {
        PanelSizingCalculator.Size size = calculator.calculate(1400, 900, 100, 100);

        assertTrue(size.width <= 1400);
        assertTrue(size.height <= 900);
    }
}
