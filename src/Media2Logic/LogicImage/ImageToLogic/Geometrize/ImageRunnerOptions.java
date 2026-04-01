package Media2Logic.LogicImage.ImageToLogic.Geometrize;

public class ImageRunnerOptions {
    public int[] shapeTypes = new int[]{0, 1, 2};
    
    /** Alpha 透明度 (0-255) */
    public int alpha = 128;
    
    /** 每步尝试的候选形状数量 */
    public int shapeCount = 50;
    
    /** 每个形状的最大变异次数 */
    public int maxShapeMutations = 100;
    
    //495对应一个逻辑
    public int maxSteps = 495;
}
