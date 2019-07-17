package org.gutkyu.dosboxj.cpu;

public final class LazyFlags {

    public Register.GeneralReg32 Var1, Var2, Res;
    public Flags.TypeFlag Type;
    public Flags.TypeFlag PrevType;
    public int oldCF;

    public LazyFlags() {
        this.Var1 = new Register.GeneralReg32();
        this.Var2 = new Register.GeneralReg32();
        this.Res = new Register.GeneralReg32();
        this.Type = Flags.TypeFlag.UNKNOWN;
        this.PrevType = Flags.TypeFlag.UNKNOWN;
        this.oldCF = 0;
    }

    public LazyFlags deepCopy() {
        LazyFlags obj = new LazyFlags();
        obj.Var1.setDWord(this.Var1.getDWord());
        obj.Var2.setDWord(this.Var2.getDWord());
        obj.Res.setDWord(this.Res.getDWord());
        obj.Type = this.Type;
        obj.PrevType = this.Type;
        obj.oldCF = this.oldCF;
        return obj;
    }

    // TODO 용도 불명, 확인 필요
    public static LazyFlags lfags;
}
