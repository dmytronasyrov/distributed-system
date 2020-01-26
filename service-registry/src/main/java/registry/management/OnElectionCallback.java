package registry.management;

public interface OnElectionCallback {
  void onElectedToBeLeader();
  void onWorker();
}
