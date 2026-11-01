package com.mrlacar.univom.api.capability;

import org.jetbrains.annotations.Nullable;

public interface IBeamReceiver {
    @Nullable
    IBeamProvider getBeamProvider();
}
