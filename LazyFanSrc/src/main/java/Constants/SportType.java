package Constants;

public enum SportType {
  MLB {
    @Override
    public boolean isOvertime(int periodNumber) {
      return periodNumber > 9;
    }

    @Override
    public int getLastPeriod() { return 9; }

    @Override
    public int getScoreThreshold() { return 1; }
  },
  NBA {
    @Override
    public int getScoreThreshold() { return 3; }
  },

  NHL {
    @Override
    public boolean isOvertime(int periodNumber) {
      return periodNumber > 3;
    }

    @Override
    public int getLastPeriod() { return 3; }

  },
  NFL {
    @Override
    public int getScoreThreshold() { return 3; }
  },
  NCAA_BASKETBALL {
    @Override
    public String espnFormat() {
      return "ncb";
    }

    @Override
    public boolean isOvertime(int periodNumber) { return periodNumber > 2; }

    @Override
    public int getLastPeriod() { return 2; }

    @Override
    public int getScoreThreshold() { return 3; }
  },
  NCAA_FOOTBALL {
    @Override
    public String espnFormat() {
      return "ncf";
    }

    @Override
    public int getScoreThreshold() { return 3; }
  },
  WNBA {
    @Override
    public int getScoreThreshold() { return 3; }
  };

  public boolean isOvertime(int periodNumber) {
    return periodNumber > 4;
  }

  public String espnFormat(){
    return this.name().toLowerCase();
  }

  public int getLastPeriod() { return 4; }

  public int getScoreThreshold() { return 0; }
}
