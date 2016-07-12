package Constants;

public enum SportType {
  MLB {
    @Override
    public boolean isOvertime(int periodNumber) {
      return periodNumber > 9;
    }
  },
  NBA,

  NHL {
    @Override
    public boolean isOvertime(int periodNumber) {
      return periodNumber > 3;
    }
  },
  NFL,
  NCAA_BASKETBALL,
  NCAA_FOOTBALL,
  WNBA;

  public boolean isOvertime(int periodNumber) {
    return periodNumber > 4;
  }
}
