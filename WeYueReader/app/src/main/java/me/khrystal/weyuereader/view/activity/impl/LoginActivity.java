package me.khrystal.weyuereader.view.activity.impl;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.OnClick;
import me.khrystal.weyuereader.R;
import me.khrystal.weyuereader.utils.ToastUtils;
import me.khrystal.weyuereader.view.base.BaseActivity;
import me.khrystal.weyuereader.viewmodel.activity.VMUseLoginInfo;
import me.khrystal.weyuereader.widget.theme.ColorTextView;

/**
 * usage:
 * author: kHRYSTAL
 * create time: 18/5/7
 * update time:
 * email: 723526676@qq.com
 */

public class LoginActivity extends BaseActivity {

    @BindView(R.id.actv_username)
    AutoCompleteTextView actvUsername;
    @BindView(R.id.et_password)
    EditText etPassword;
    @BindView(R.id.ctv_register)
    ColorTextView ctvRegister;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    private VMUseLoginInfo mModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new VMUseLoginInfo(this);
        setBinddingView(R.layout.activity_login, NO_BINDDING, mModel);
    }

    @Override
    protected void initView() {
        super.initView();
        initThemeToolBar("用户登录");
    }

    @OnClick({R.id.ctv_register, R.id.fab})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.ctv_register:
                startActivityForResult(new Intent(this, RegisterActivity.class), 10000);
                break;
            case R.id.fab:
                String username = actvUsername.getText().toString();
                String password = etPassword.getText().toString();

                if (TextUtils.isEmpty(username)) {
                    ToastUtils.show("用户名不能为空");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    ToastUtils.show("密码不能为空");
                    return;
                }
                mModel.login(username, password);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == 10000 && data != null) {
            String username = data.getStringExtra("username");
            actvUsername.setText(username != null ? username : "");
            String password = data.getStringExtra("password");
            etPassword.setText(password != null ? password : "");
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
}
