package net.ccbluex.liquidbounce.utils.math;

public class AimResult {
    public AimAnalysisEngine.HeuristicsResult result;
    public String description;

    public AimResult(AimAnalysisEngine.HeuristicsResult result, String description) {
        this.result=result;
        this.description=description;
    }
}
