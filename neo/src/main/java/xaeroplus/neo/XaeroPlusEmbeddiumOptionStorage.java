package xaeroplus.neo;

import org.embeddedt.embeddium.api.options.structure.OptionStorage;

public class XaeroPlusEmbeddiumOptionStorage implements OptionStorage<Void> {
    public static final XaeroPlusEmbeddiumOptionStorage INSTANCE = new XaeroPlusEmbeddiumOptionStorage();

    @Override
    public Void getData() {
        return null;
    }

    @Override
    public void save() {

    }
}
