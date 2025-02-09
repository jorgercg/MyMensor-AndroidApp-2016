#ifndef VP_CONFIGURE_FILTER
#define VP_CONFIGURE_FILTER

#include <vector>

#include <android/log.h>
#define  LOG_TAG    "NATIVE VpCfgFilter"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


#include <opencv2/core/core.hpp>
#include <opencv2/features2d/features2d.hpp>

namespace mymensor {

class VpConfigureFilter
{
public:
    VpConfigureFilter(cv::Mat &referenceImageBGR, double realSize);
    float *getPose();
    void apply(cv::Mat &src, cv::Mat &projection);

private:
    void findPose(cv::Mat &projection);
    void draw(cv::Mat src, int isHudOn);

    // The reference image (this detector's target).
    cv::Mat mReferenceImage;
    // Features of the reference image.
    std::vector<cv::KeyPoint> mReferenceKeypoints;
    // Descriptors of the reference image's features.
    cv::Mat mReferenceDescriptors;
    // The corner coordinates of the reference image, in pixels.
    cv::Mat mReferenceCorners;
    // The reference image's corner coordinates, in 3D, in real
    // units.
    std::vector<cv::Point3f> mReferenceCorners3D;

    // Features of the scene (the current frame).
    std::vector<cv::KeyPoint> mSceneKeypoints;
    // Descriptors of the scene's features.
    cv::Mat mSceneDescriptors;
    // Tentative corner coordinates detected in the scene, in
    // pixels.
    cv::Mat mCandidateSceneCorners;

    // A grayscale version of the scene.
    cv::Mat mGraySrc;
    // Tentative matches of scene features and reference features.
    std::vector<cv::DMatch> mMatches;

    // A feature detector, which finds features in images, and
    // descriptor extractor, which creates descriptors of features.
    cv::Ptr<cv::Feature2D> mFeatureDetectorAndDescriptorExtractor;
    // A descriptor matcher, which matches features based on their
    // descriptors.
    cv::Ptr<cv::DescriptorMatcher> mDescriptorMatcher;

    // Distortion coefficients of the camera's lens.
    cv::Mat mDistCoeffs;

    // The Euler angles of the detected target.
    cv::Mat mRVec;
    // The XYZ coordinates of the detected target.
    cv::Mat mTVec;
    // The rotation matrix of the detected target.
    cv::Mat mRotation;
    // The OpenGL pose matrix of the detected target.
    float mPose[7];

    // Whether the target is currently detected.
    bool mTargetFound;

    CvRect rect;
};

} // namespace mymensor

#endif // VP_CONFIGURE_FILTER
