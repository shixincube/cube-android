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

package com.shixincube.app.ui.activity;

import android.graphics.Color;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.ferry.handler.BoxReportHandler;
import cube.ferry.model.BoxReport;

/**
 * 存储空间界面。
 */
public class BoxStorageActivity extends BaseActivity {

    @BindView(R.id.pcStorageSize)
    PieChart sizeChart;

    public BoxStorageActivity() {
        super();
    }

    @Override
    public void initView() {
        this.setToolbarTitle(UIUtils.getString(R.string.box_storage_management));

        this.initSpaceSizeChart();
    }

    @Override
    public void initListener() {
    }

    @Override
    public void initData() {
        CubeEngine.getInstance().getFerryService().getBoxReport(new BoxReportHandler(true) {
            @Override
            public void handleBoxReport(BoxReport boxReport) {
                refreshData(boxReport);
            }
        }, new DefaultFailureHandler(true) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                UIUtils.showToast(UIUtils.getString(R.string.toast_get_report_failed));
            }
        });
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_box_storage;
    }

    private void initSpaceSizeChart() {
        // 不使用百分比
        this.sizeChart.setUsePercentValues(false);
        // 显示描述
        this.sizeChart.getDescription().setEnabled(false);
        // 设置背景颜色
        this.sizeChart.setBackgroundColor(UIUtils.getColorByAttrId(R.attr.colorBackground));
        // 设置外边距
        this.sizeChart.setExtraOffsets(0, 2, 0, 2);
        // 旋转起始角度
        this.sizeChart.setRotationAngle(0);
        // 是否可手动旋转
        this.sizeChart.setRotationEnabled(true);
        // 点击 Item 是否高亮
        this.sizeChart.setHighlightPerTapEnabled(true);

        // 设置展开动画
        this.sizeChart.animateY(1200, Easing.EaseInOutQuad);

        // 是否显示 Item 标签的文本
        this.sizeChart.setDrawEntryLabels(true);
        this.sizeChart.setEntryLabelColor(Color.WHITE);
        this.sizeChart.setEntryLabelTextSize(12);

        // 是否显示内环
        this.sizeChart.setDrawHoleEnabled(true);
        this.sizeChart.setHoleRadius(38f);
        this.sizeChart.setTransparentCircleRadius(42f);
        this.sizeChart.setCenterText(UIUtils.getString(R.string.chart_storage_size_name));
        this.sizeChart.setCenterTextColor(UIUtils.getColorByAttrId(R.attr.colorText));
        this.sizeChart.setCenterTextSize(16);
    }

    private void refreshData(BoxReport boxReport) {
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(UIUtils.getColor(R.color.chart_color_1));
        colors.add(UIUtils.getColor(R.color.chart_color_2));
        colors.add(UIUtils.getColor(R.color.chart_color_3));
        colors.add(UIUtils.getColor(R.color.chart_color_4));
        colors.add(UIUtils.getColor(R.color.chart_color_5));
        colors.add(UIUtils.getColor(R.color.chart_color_6));
        colors.add(UIUtils.getColor(R.color.chart_color_8));
        colors.add(UIUtils.getColor(R.color.chart_color_9));

        PieDataSet dataSet = new PieDataSet(makePieData(boxReport), "");
        // 饼状 Item 之间的间隙
        dataSet.setSliceSpace(3f);
        // 饼状 Item 被选中变化的移动距离
        dataSet.setSelectionShift(10f);
        // 设置饼状颜色
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        // 显示数据实体值
        data.setDrawValues(true);
        // 文本颜色
        data.setValueTextColor(UIUtils.getColorByAttrId(R.attr.colorText));
        // 文本大小
        data.setValueTextSize(14);
        data.setValueFormatter(new DiskSpaceFormatter());

        Legend legend = this.sizeChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        // 图例图形样式
        legend.setForm(Legend.LegendForm.CIRCLE);
        // 图例图形大小
        legend.setFormSize(12);
        legend.setDrawInside(true);
        legend.setWordWrapEnabled(true);
        legend.setTextColor(UIUtils.getColorByAttrId(R.attr.colorText));
        legend.setTextSize(14);

        this.sizeChart.setData(data);
        this.sizeChart.invalidate();
    }

    private List<PieEntry> makePieData(BoxReport boxReport) {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(toMB(boxReport.getDataSpaceSize()),
                UIUtils.getString(R.string.box_chart_item_data_size)));

        pieEntries.add(new PieEntry(toMB(boxReport.getImageFilesUsedSize()),
                UIUtils.getString(R.string.box_chart_item_image_file)));
        pieEntries.add(new PieEntry(toMB(boxReport.getDocFilesUsedSize()),
                UIUtils.getString(R.string.box_chart_item_doc_file)));
        pieEntries.add(new PieEntry(toMB(boxReport.getVideoFilesUsedSize()),
                UIUtils.getString(R.string.box_chart_item_video_file)));
        pieEntries.add(new PieEntry(toMB(boxReport.getAudioFilesUsedSize()),
                UIUtils.getString(R.string.box_chart_item_audio_file)));
        pieEntries.add(new PieEntry(toMB(boxReport.getPackageFilesUsedSize()),
                UIUtils.getString(R.string.box_chart_item_package_file)));
        pieEntries.add(new PieEntry(toMB(boxReport.getOtherFilesUsedSize()),
                UIUtils.getString(R.string.box_chart_item_other_file)));

        pieEntries.add(new PieEntry(toMB(boxReport.getFreeDiskSize()),
                UIUtils.getString(R.string.box_chart_item_free_size)));

        return pieEntries;
    }

    private int toMB(long size) {
        long mb = Math.round(size / (1024f * 1024f));
        return (int) mb;
    }

    protected class DiskSpaceFormatter extends ValueFormatter {

        public DecimalFormat format;

        public DiskSpaceFormatter() {
            format = new DecimalFormat("###,###,##0.0");
        }

        @Override
        public String getPieLabel(float value, PieEntry pieEntry) {
            if (value > 1024.0f) {
                return this.format.format(value / 1024.0f) + "GB";
            }
            else {
                return this.format.format(value) + "MB";
            }
        }
    }
}
