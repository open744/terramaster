import java.io.File;
import java.util.Collection;


public interface TileService {

	void setScnPath(File file);

	void start();

	void sync(Collection<TileName> set);

	Collection<TileName> getSyncList();

	void quit();

	void cancel();

	void delete(Collection<TileName> selection);

  void setTypes(boolean selected, boolean selected2, boolean selected3);
}
