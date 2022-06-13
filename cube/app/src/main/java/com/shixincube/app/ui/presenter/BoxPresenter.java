/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.shixincube.app.ui.presenter;

import com.shixincube.app.ui.activity.BoxActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.BoxView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cube.engine.CubeEngine;
import cube.ferry.handler.DomainInfoHandler;
import cube.ferry.model.DomainInfo;

public class BoxPresenter extends BasePresenter<BoxView> {

    private final static String TAG = BoxPresenter.class.getSimpleName();

    private SimpleDateFormat dateFormat;

    public BoxPresenter(BoxActivity activity) {
        super(activity);
        this.dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分", Locale.CHINA);
    }

    public void loadData() {
        CubeEngine.getInstance().getFerryService().getDomainInfo(new DomainInfoHandler(true) {
            @Override
            public void handleDomainInfo(DomainInfo domainInfo) {
                if (null == domainInfo) {
                    return;
                }

                getView().getDomainNameView().setEndText(domainInfo.getDomainName());

                getView().getDomainBeginningView().setEndText(
                        dateFormat.format(new Date(domainInfo.getBeginning())));

                getView().getDomainEndingView().setEndText(
                        dateFormat.format(new Date(domainInfo.getBeginning() + domainInfo.getDuration())));

                getView().getDomainLimitView().setEndText(
                        Integer.toString(domainInfo.getLimit()) + " 人");
            }
        });
    }
}
